package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import com.shilapi.xcertplay.media.CarPlayCall
import com.shilapi.xcertplay.media.CarPlayCallChange
import com.shilapi.xcertplay.media.CarPlayCallEvent

/**
 * MG4 test (off by default): shows each CarPlay call to the car's Telecom, as the stock AllGo
 * CarPlay app does (`CarPlayTelephonyManager`, `MirroringControllerService`). The car's phone
 * system, call audio policy and calling screens then see a call in progress. Not yet tried in
 * the car.
 *
 * A `CarPlayPhone` account with capability CALL_PROVIDER is registered when CarPlay connects.
 * A ringing call goes to `addNewIncomingCall` with an external ringer (the iPhone's ring plays
 * over CarPlay audio); an outgoing or running call goes to the hidden `addNewUnknownCall` by
 * reflection. Every failure is logged and swallowed: a call that cannot be mirrored still works
 * over CarPlay.
 */
internal object CarPlayTelephony {
    private const val TAG = "DiPlay-Telephony"
    internal const val ACCOUNT_ID = "CarPlayPhone"
    internal const val EXTRA_REMOTE_ID = "CARPLAY_CALL_REMOTE_ID"
    internal const val EXTRA_UUID = "CARPLAY_CALL_UUID"
    internal const val EXTRA_NAME = "CARPLAY_CALL_NAME"
    internal const val EXTRA_STATE = "CARPLAY_CALL_STATE"
    internal const val EXTRA_CARPLAY_CALL = "carplay_call"
    private const val EXTRA_EXTERNAL_RINGER = "android.telecom.extra.CALL_EXTERNAL_RINGER"

    private val connections = LinkedHashMap<String, CarPlayCallConnection>()
    private var appContext: Context? = null
    private var handle: PhoneAccountHandle? = null
    private var report: (String) -> Unit = {}
    private var hookSwitch: () -> Boolean = { false }

    /** The Telecom connection for [uuid], created before Telecom asks the service for it. */
    @Synchronized
    internal fun connection(uuid: String?): CarPlayCallConnection? = connections[uuid]

    internal fun log(message: String) {
        Log.i(TAG, message)
        runCatching { report(message) }
    }

    /**
     * Registers the account. [hook] sends CarPlay's telephone hook switch (answer or end), used
     * when the car's own call screen answers or hangs up a mirrored call.
     */
    @Synchronized
    fun start(context: Context, log: (String) -> Unit, hook: () -> Boolean) {
        report = log
        hookSwitch = hook
        appContext = context.applicationContext
        val telecom = appContext?.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecom == null) {
            log("Telecom: unavailable; calls are not shown to the car")
            return
        }
        val accountHandle = PhoneAccountHandle(ComponentName(appContext!!, CarPlayConnectionService::class.java), ACCOUNT_ID)
        val steps = StringBuilder()
        runCatching {
            val account = PhoneAccount.Builder(accountHandle, ACCOUNT_ID)
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .build()
            telecom.registerPhoneAccount(account)
            steps.append("registered ")
            runCatching { telecomCall(telecom, "enablePhoneAccount", accountHandle, true); steps.append("enabled ") }
            runCatching { telecomCall(telecom, "setUserSelectedOutgoingPhoneAccount", accountHandle); steps.append("selected ") }
            handle = accountHandle
        }.onFailure {
            handle = null
            log("Telecom: account registration failed ${it.javaClass.simpleName}")
            return
        }
        log("Telecom: CarPlayPhone account ${steps.toString().trim()}")
    }

    /** Hidden or permission-gated TelecomManager methods are called by reflection; a failure is logged by the caller's step list. */
    private fun telecomCall(telecom: TelecomManager, name: String, vararg args: Any) {
        val method = telecom.javaClass.methods.first { it.name == name && it.parameterTypes.size == args.size }
        method.invoke(telecom, *args)
    }

    /** Mirrors one call change into Telecom. Safe to call from any thread. */
    fun onChange(change: CarPlayCallChange) {
        val context: Context
        val accountHandle: PhoneAccountHandle
        synchronized(this) {
            context = appContext ?: return
            accountHandle = handle ?: return
        }
        val call = change.call
        log("Telecom: call ${change.event} status=${call.status} direction=${call.direction} uuid=${call.uuid.take(8)}")
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
        when (change.event) {
            CarPlayCallEvent.INCOMING, CarPlayCallEvent.OUTGOING -> {
                val connection = CarPlayCallConnection(call)
                synchronized(this) { connections[call.uuid] = connection }
                val extras = extrasFor(call)
                runCatching {
                    if (change.event == CarPlayCallEvent.INCOMING) {
                        extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, Uri.fromParts("tel", call.remoteId, null))
                        extras.putBoolean(EXTRA_EXTERNAL_RINGER, true)
                        telecom.addNewIncomingCall(accountHandle, extras)
                    } else {
                        extras.putParcelable(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, Uri.fromParts("tel", call.remoteId, null))
                        val method = telecom.javaClass.getMethod("addNewUnknownCall", PhoneAccountHandle::class.java, Bundle::class.java)
                        method.invoke(telecom, accountHandle, extras)
                    }
                    log("Telecom: ${if (change.event == CarPlayCallEvent.INCOMING) "addNewIncomingCall" else "addNewUnknownCall"} sent")
                }.onFailure {
                    log("Telecom: add call failed ${it.javaClass.simpleName} ${it.cause?.javaClass?.simpleName.orEmpty()}")
                    synchronized(this) { connections.remove(call.uuid) }
                }
            }
            CarPlayCallEvent.UPDATED -> synchronized(this) { connections[call.uuid] }?.update(call)
            CarPlayCallEvent.ENDED -> {
                synchronized(this) { connections.remove(call.uuid) }?.end(DisconnectCause.REMOTE)
            }
        }
    }

    /** The CarPlay session stopped: disconnect and destroy every mirrored call. */
    fun stop() {
        val open = synchronized(this) {
            val list = connections.values.toList()
            connections.clear()
            val had = appContext != null
            appContext = null
            handle = null
            hookSwitch = { false }
            if (!had && list.isEmpty()) return
            list
        }
        open.forEach { it.end(DisconnectCause.LOCAL) }
        log("Telecom: session stopped; disconnected ${open.size} call(s)")
    }

    internal fun sendHook(reason: String): Boolean {
        val sent = hookSwitch()
        log("Telecom: $reason from the car -> CarPlay hook switch sent=$sent")
        return sent
    }

    private fun extrasFor(call: CarPlayCall) = Bundle().apply {
        putString(EXTRA_REMOTE_ID, call.remoteId)
        putString(EXTRA_UUID, call.uuid)
        putString(EXTRA_NAME, call.displayName)
        putInt(EXTRA_STATE, call.status)
        putBoolean(EXTRA_CARPLAY_CALL, true)
    }
}

/** One mirrored CarPlay call. Answering or hanging up on the car's screen presses CarPlay's hook switch. */
internal class CarPlayCallConnection(private var call: CarPlayCall) : Connection() {
    @Volatile private var finished = false

    /** Fills in the address and name, then follows the call's state. */
    fun attach() {
        setAddress(Uri.fromParts("tel", call.remoteId, null), TelecomManager.PRESENTATION_ALLOWED)
        setCallerDisplayName(call.displayName, TelecomManager.PRESENTATION_ALLOWED)
        apply(call)
    }

    fun update(next: CarPlayCall) {
        call = next
        apply(next)
    }

    private fun apply(state: CarPlayCall) {
        if (finished) return
        when (state.status) {
            CarPlayCall.STATUS_RINGING -> setRinging()
            CarPlayCall.STATUS_SENDING, CarPlayCall.STATUS_CONNECTING ->
                if (state.direction == CarPlayCall.DIRECTION_OUTGOING) setDialing()
            CarPlayCall.STATUS_ACTIVE -> setActive()
            CarPlayCall.STATUS_HELD -> setOnHold()
        }
    }

    fun end(cause: Int) {
        if (finished) return
        finished = true
        runCatching {
            setDisconnected(DisconnectCause(cause))
            destroy()
        }
    }

    override fun onAnswer() {
        CarPlayTelephony.sendHook("answer")
    }

    override fun onReject() {
        CarPlayTelephony.sendHook("reject")
        end(DisconnectCause.REJECTED)
    }

    override fun onDisconnect() {
        if (!finished) CarPlayTelephony.sendHook("hang up")
        end(DisconnectCause.LOCAL)
    }

    override fun onAbort() = onDisconnect()

    override fun onHold() {
        CarPlayTelephony.log("Telecom: hold from the car ignored (CarPlay hold is not wired)")
    }

    override fun onUnhold() {
        CarPlayTelephony.log("Telecom: unhold from the car ignored (CarPlay hold is not wired)")
    }
}

/** Telecom's door to the mirrored calls; looks each one up by the iPhone's call UUID. */
class CarPlayConnectionService : ConnectionService() {
    private fun find(request: ConnectionRequest?): CarPlayCallConnection? {
        val extras = request?.extras ?: return null
        val uuid = extras.getString(CarPlayTelephony.EXTRA_UUID)
            ?: extras.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)?.getString(CarPlayTelephony.EXTRA_UUID)
        val connection = CarPlayTelephony.connection(uuid)
        CarPlayTelephony.log("Telecom: service create connection uuid=${uuid?.take(8)} found=${connection != null}")
        return connection?.also { it.attach() }
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection =
        find(request) ?: Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection =
        find(request) ?: Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))

    // Hidden in the SDK: no override keyword, so the JVM still dispatches Telecom's call to it.
    fun onCreateUnknownConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection =
        find(request) ?: Connection.createFailedConnection(DisconnectCause(DisconnectCause.ERROR))
}
