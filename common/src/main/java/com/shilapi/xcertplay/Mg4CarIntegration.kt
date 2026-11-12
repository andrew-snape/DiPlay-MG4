package com.shilapi.xcertplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.shilapi.xcertplay.media.AndroidMediaSink
import com.shilapi.xcertplay.orchestration.CarPlayController

/** How the car's vendor call listener reports a call, from the stock app's `onCallStatusChanged`. */
internal object VendorCallStatus {
    /** Listen mask the stock app passes to `TelephonyManager.listen` to get `onCallStatusChanged`. */
    const val LISTEN_MASK = 0x60000
    const val TYPE_NATIVE_BLUETOOTH = 0
    const val TYPE_CARPLAY = 2
    private const val STATE_RINGING = 4
    private const val STATE_OFFHOOK = 5

    /** The car's own hands-free call is ringing or on. Idle, or any other state, is not. */
    fun nativeCallActive(state: Int, type: Int): Boolean? = when {
        type != TYPE_NATIVE_BLUETOOTH -> null
        state == STATE_RINGING || state == STATE_OFFHOOK -> true
        else -> false
    }

    fun describe(state: Int, type: Int): String {
        val kind = when (type) {
            TYPE_NATIVE_BLUETOOTH -> "native Bluetooth"
            TYPE_CARPLAY -> "CarPlay"
            else -> "other"
        }
        val name = when (state) {
            STATE_RINGING -> "ringing"
            STATE_OFFHOOK -> "offhook"
            7 -> "idle"
            else -> "state $state"
        }
        return "$kind $name"
    }
}

/** What to do with the video when the car's display reports on or off. */
internal object DisplayReport {
    /**
     * The stock app treats `state=true` as the display going off (it gives the screen back to the
     * car) and `false` as on (it asks for the CarPlay screen again). Read from the decompile only:
     * not yet confirmed in the car, so the log names both.
     */
    fun displayOff(stateExtra: Boolean): Boolean = stateExtra

    /** Ask the iPhone for a fresh key frame when the display comes back, so the picture returns at once. */
    fun shouldRequestKeyFrame(previousOff: Boolean?, nowOff: Boolean): Boolean = previousOff == true && !nowOff
}

/**
 * MG4 car hooks that copy the stock AllGo CarPlay app, all read-only or reversible and attached
 * with the CarPlay session ([attach]) and released with it ([detach]):
 *
 * - Telecom mirroring of CarPlay calls (setting "Show calls to the car", off by default);
 * - the vendor call listener (`onCallStatusChanged`, mask 0x60000), logged, which releases CarPlay
 *   audio focus while a native Bluetooth call is on and restores it afterwards;
 * - the display report broadcast `com.saic.conn.display.report`, logged, with a key frame request
 *   when the display comes back. The session and the video decoder are never torn down.
 *
 * Does nothing outside MG4 firmware, and every step fails closed.
 */
internal object Mg4CarIntegration {
    private const val TAG = "DiPlay-Mg4Car"
    private const val ACTION_DISPLAY_REPORT = "com.saic.conn.display.report"
    private const val EXTRA_STATE = "state"
    /** The main screen stream type, as in CarPlayHostActivity. */
    private const val SCREEN_TYPE_MAIN = 110

    private val mainHandler = Handler(Looper.getMainLooper())
    private var controller: CarPlayController? = null
    private var appContext: Context? = null
    private var listener: PhoneStateListener? = null
    private var displayReceiver: BroadcastReceiver? = null
    private var displayOff: Boolean? = null
    private var showCalls = false

    @Synchronized
    fun attach(context: Context, next: CarPlayController, sink: AndroidMediaSink, log: (String) -> Unit) {
        if (!Mg4FirmwareProfile.detect().isMg4) return
        releaseLocked()
        val app = context.applicationContext
        appContext = app
        controller = next

        showCalls = AirPlayPersistence.loadShowCallsToCar(app)
        log("Car integration: show calls to the car=${if (showCalls) "on" else "off"} " +
            "music tag=${if (AirPlayPersistence.loadTagMusicAsCarPlay(app)) "on" else "off"} " +
            "car phone mode=${if (AirPlayPersistence.loadCarPhoneMode(app)) "on" else "off"} " +
            "microphone=${if (AirPlayPersistence.loadMicrophoneSource(app) == 3) "stock" else "other"}")
        if (showCalls) {
            CarPlayTelephony.start(app, log) { next.sendTelephonyButton(CarPlayMediaKeys.TELEPHONY_HOOK_SWITCH) }
            next.callListener = CarPlayTelephony::onChange
        }

        registerVendorListener(app, sink, log)
        registerDisplayReceiver(app, sink, log)
    }

    /** Ends the hooks for [expected]; a newer session's hooks are left alone. */
    @Synchronized
    fun detach(expected: CarPlayController?) {
        if (expected == null || controller !== expected) return
        releaseLocked()
    }

    private fun releaseLocked() {
        controller?.callListener = null
        if (showCalls) CarPlayTelephony.stop()
        showCalls = false
        controller = null
        val app = appContext
        listener?.let { old ->
            mainHandler.post { runCatching { app?.getSystemService(TelephonyManager::class.java)?.listen(old, PhoneStateListener.LISTEN_NONE) } }
        }
        listener = null
        displayReceiver?.let { receiver -> runCatching { app?.unregisterReceiver(receiver) } }
        displayReceiver = null
        displayOff = null
        appContext = null
    }

    private fun registerVendorListener(app: Context, sink: AndroidMediaSink, log: (String) -> Unit) {
        // PhoneStateListener binds to the calling thread's looper, so create it on the main thread.
        mainHandler.post {
            runCatching {
                val telephony = app.getSystemService(TelephonyManager::class.java)
                    ?: return@runCatching log("Vendor call listener: no TelephonyManager")
                val created = VendorCallListener(sink, log)
                telephony.listen(created, VendorCallStatus.LISTEN_MASK)
                synchronized(this) { if (appContext === app) listener = created else telephony.listen(created, PhoneStateListener.LISTEN_NONE) }
                log("Vendor call listener: registered mask=0x${VendorCallStatus.LISTEN_MASK.toString(16)}")
            }.onFailure {
                Log.w(TAG, "vendor call listener failed", it)
                log("Vendor call listener: not registered (${it.javaClass.simpleName})")
            }
        }
    }

    /**
     * The hidden `onCallStatusChanged(int state, int type, String number)` is not in the public
     * SDK. It is declared here without `override`, so on a framework that has it the JVM calls
     * this method; on one that does not, it is never called and nothing breaks.
     */
    private class VendorCallListener(
        private val sink: AndroidMediaSink,
        private val log: (String) -> Unit,
    ) : PhoneStateListener() {
        @Suppress("unused")
        fun onCallStatusChanged(state: Int, type: Int, incomingNumber: String?) {
            runCatching {
                // The number is not logged.
                log("Vendor call status: state=$state type=$type (${VendorCallStatus.describe(state, type)})")
                VendorCallStatus.nativeCallActive(state, type)?.let { active -> sink.setNativeCallActive(active) }
            }
        }
    }

    private fun registerDisplayReceiver(app: Context, sink: AndroidMediaSink, log: (String) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.hasExtra(EXTRA_STATE)) {
                    log("Display report: no state extra")
                    return
                }
                val raw = intent.getBooleanExtra(EXTRA_STATE, false)
                val off = DisplayReport.displayOff(raw)
                val previous = synchronized(this@Mg4CarIntegration) { displayOff.also { displayOff = off } }
                val keyFrame = DisplayReport.shouldRequestKeyFrame(previous, off)
                log("Display report: state=$raw -> display ${if (off) "off" else "on"} (state=true is off in the stock app; unconfirmed)" +
                    if (keyFrame) "; key frame requested" else "")
                if (keyFrame) runCatching { sink.requestVideoRecoveryNow(SCREEN_TYPE_MAIN) }
            }
        }
        runCatching { app.registerReceiver(receiver, IntentFilter(ACTION_DISPLAY_REPORT)) }
            .onSuccess { displayReceiver = receiver }
            .onFailure { log("Display report: receiver failed ${it.javaClass.simpleName}") }
    }
}
