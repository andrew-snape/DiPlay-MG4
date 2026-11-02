package com.shilapi.xcertplay.vehicle

import android.os.SystemClock
import android.util.Log
import com.evsuite.hardware.EVHardware
import com.evsuite.hardware.saic.SaicVehicleCondition
import com.shilapi.xcertplay.transport.VehicleGear
import com.shilapi.xcertplay.transport.VehicleSpeedReading
import com.shilapi.xcertplay.transport.VehicleSpeedSample
import com.shilapi.xcertplay.transport.VehicleSpeedSource
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * MG4 speed and gear, read directly from the car (no adb). Speed is the AOSP vehicle-speed property
 * that EVHardware already scales per firmware; gear is the vehiclecondition service:
 * 1 P, 2 R, 3 N, 4 D on SWI165 (car test 2026-11-03); an unknown positive value is sent as D.
 */
internal object Mg4WheelSpeed {
    const val GEAR_PARK = 1

    fun metersPerSecond(kmh: Float?): Double? =
        kmh?.toDouble()?.takeIf { it.isFinite() && it in 0.0..300.0 }?.div(3.6)

    // Car test 2026-11-03 (mg4.92), moving the selector P, R, P, R, N, D, P: 1 P, 2 R, 3 N, 4 D.
    fun gear(raw: Int?): VehicleGear? = when (raw) {
        GEAR_PARK -> VehicleGear.PARK
        2 -> VehicleGear.REVERSE
        3 -> VehicleGear.NEUTRAL
        4 -> VehicleGear.DRIVE
        null -> null
        else -> if (raw > 0) VehicleGear.DRIVE else null
    }
}

/**
 * Wheel speed for the iPhone's dead reckoning in tunnels. Reads the speed four times a second and the
 * gear once a second, only while the iPhone asks for location.
 */
internal object Mg4WheelSpeedSource : VehicleSpeedSource {
    private const val TAG = "DiPlay-MG4-Speed"
    private const val READ_MILLIS = 250L
    private const val GEAR_EVERY_READS = 4
    private const val MAX_SAMPLES = 40

    private var executor: ScheduledExecutorService? = null
    private val samples = ArrayList<VehicleSpeedSample>()
    private var gear: VehicleGear? = null
    private var rawGear: Int? = null
    private var reads = 0
    private var firstSampleLogged = false

    /** The session log, so these lines reach the diagnostic report; logcat alone does not. */
    @Volatile var reportLog: (String) -> Unit = {}

    private fun log(message: String) {
        Log.i(TAG, message)
        reportLog("MG4 $message")
    }

    @Synchronized
    override fun start() {
        if (executor != null) return
        samples.clear()
        gear = null
        rawGear = null
        reads = 0
        firstSampleLogged = false
        executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "diplay-mg4-wheel-speed").apply { isDaemon = true }
        }.also { it.scheduleWithFixedDelay(::poll, 0, READ_MILLIS, TimeUnit.MILLISECONDS) }
        log("wheel speed started")
    }

    override fun stop() {
        val running = synchronized(this) { executor.also { executor = null } } ?: return
        running.shutdown()
        log("wheel speed stopped")
    }

    @Synchronized
    override fun drain(): VehicleSpeedReading? {
        val current = gear ?: return null
        if (samples.isEmpty()) return null
        return VehicleSpeedReading(current, samples.toList()).also { samples.clear() }
    }

    private fun poll() {
        try {
            pollSafely()
        } catch (error: Throwable) {
            Log.w(TAG, "MG4 wheel speed read failed", error)
        }
    }

    private fun pollSafely() {
        if (reads++ % GEAR_EVERY_READS == 0) {
            val raw = SaicVehicleCondition.gearOrNull()
            Mg4WheelSpeed.gear(raw)?.let { next ->
                val changed = synchronized(this) { (rawGear != raw).also { gear = next; rawGear = raw } }
                if (changed) log("gear raw=$raw sent=$next")
            }
        }
        val before = SystemClock.elapsedRealtime()
        val speed = Mg4WheelSpeed.metersPerSecond(EVHardware.getVehicleSpeedKmh()) ?: return
        val sample = VehicleSpeedSample((before + SystemClock.elapsedRealtime()) / 2, speed)
        synchronized(this) {
            if (samples.size == MAX_SAMPLES) samples.removeAt(0)
            samples += sample
        }
        if (!firstSampleLogged) {
            firstSampleLogged = true
            log("first wheel speed ${"%.1f".format(java.util.Locale.US, speed * 3.6)} km/h")
        }
    }
}
