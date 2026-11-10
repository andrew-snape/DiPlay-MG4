package com.shilapi.xcertplay.vehicle

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.shilapi.xcertplay.transport.VehicleGear
import com.shilapi.xcertplay.transport.VehicleStatusProvider
import com.shilapi.xcertplay.transport.VehicleStatusSnapshot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Read-only MG4 (SWI165) vehicle status for Apple Maps. Other firmware reads nothing. */
object Mg4Vehicle : VehicleStatusProvider {
    private const val TAG = "DiPlay-MG4"
    private const val READ_MILLIS = 30_000L
    private const val STALE_MILLIS = 3 * 60_000L
    private const val INITIAL_READ_TIMEOUT_MILLIS = 8_000L
    private const val INITIAL_RETRY_MILLIS = 500L

    @Volatile private var app: Context? = null
    @Volatile private var latest: VehicleStatusSnapshot? = null
    @Volatile private var latestMillis = 0L
    @Volatile private var started = false
    @Volatile private var initialized = false
    @Volatile private var reportedCharge: Pair<Int?, Boolean>? = null
    private val reader = Mg4CarReader(Mg4CarSignals)

    /**
     * The session log, so charging changes reach the diagnostic report; logcat alone does not. Setting
     * it logs the next reading even if unchanged, so each session starts with the current state.
     */
    @Volatile var reportLog: (String) -> Unit = {}
        set(value) {
            field = value
            reportedCharge = null
        }
    private val firstReading = CountDownLatch(1)
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "diplay-mg4-vehicle").apply { isDaemon = true }
    }

    fun available(context: Context): Boolean = Mg4Firmware.supported()

    @Synchronized
    fun start(context: Context) {
        app = context.applicationContext
        if (started) {
            if (initialized) executor.execute(::poll)
            return
        }
        started = true
        executor.execute {
            try {
                val hub = SaicBinder.connect(context.applicationContext)
                val properties = CarProperties.connect(context.applicationContext)
                Mg4CarSignals.sources.clear()
                log("hub=$hub properties=$properties")
                initialized = true
                primeFirstReading()
                log("first reading " + Mg4CarSignals.sources.entries.joinToString(" ") { "${it.key}=${it.value}" })
                executor.scheduleWithFixedDelay(::poll, READ_MILLIS, READ_MILLIS, TimeUnit.MILLISECONDS)
            } catch (error: Throwable) {
                Log.e(TAG, "MG4 car reads could not start; vehicle data disabled", error)
            } finally {
                firstReading.countDown()
            }
        }
    }

    override fun snapshot(): VehicleStatusSnapshot? {
        // Identification runs off the UI thread. Give the first read time to finish before
        // iAP2 permanently decides whether this CarPlay connection is an EV session.
        if (started && latest == null) {
            runCatching { firstReading.await(INITIAL_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) }
        }
        return latest?.takeIf { SystemClock.elapsedRealtime() - latestMillis <= STALE_MILLIS }
    }

    /** Fail closed: video remains disabled if the gear cannot be read. */
    fun parked(context: Context): Boolean? {
        if (!available(context)) return null
        return runCatching {
            Mg4WheelSpeed.knownGear(Mg4CarSignals.rawGear())?.let { it == VehicleGear.PARK }
        }
            .onFailure { Log.e(TAG, "Could not read MG4 gear", it) }
            .getOrNull()
    }

    private fun poll() {
        try {
            pollSafely()
        } catch (error: Throwable) {
            Log.e(TAG, "MG4 telemetry read failed", error)
        }
    }

    private fun primeFirstReading() {
        val deadline = SystemClock.elapsedRealtime() + INITIAL_READ_TIMEOUT_MILLIS
        do {
            pollSafely()
            if (latest != null) return
            Thread.sleep(INITIAL_RETRY_MILLIS)
        } while (SystemClock.elapsedRealtime() < deadline)
    }

    private fun pollSafely() {
        val context = app ?: return
        if (!Mg4Firmware.supported()) return
        val vehicle = reader.read()
        val percent = vehicle.socPercent?.toDouble() ?: return
        val range = vehicle.rangeKm ?: return
        val fraction = percent / 100.0
        val currentKwh = vehicle.batteryEnergyKwh?.toDouble()
        val capacityKwh = vehicle.batteryCapacityKwh?.toDouble()
            ?: currentKwh?.takeIf { fraction > 0.0 }?.div(fraction)
        val maxRange = if (fraction > 0.0) (range / fraction).roundToInt() else range
        val reportedCapacityKwh = capacityKwh ?: 100.0
        val reportedCurrentKwh = currentKwh ?: reportedCapacityKwh * fraction
        latest = VehicleStatusSnapshot(
            rangeKm = range,
            rangeWarning = percent <= VehicleSettings.lowChargePercent(context),
            batteryPercent = percent,
            currentChargeWh = (reportedCurrentKwh * 1000).roundToLong(),
            maxChargeWh = (reportedCapacityKwh * 1000).roundToLong(),
            maxRangeKm = maxRange,
            charging = Mg4Charging.isCharging(
                vehicle.chargingStatus,
                vehicle.chargePortConnected,
                vehicle.batteryPowerKw,
            ),
        )
        latestMillis = SystemClock.elapsedRealtime()
        val line = "battery ${percent.roundToInt()}% range ${range}km " +
            "chargeStatus=${vehicle.chargingStatus} charging=${latest?.charging}"
        Log.i(TAG, "${Mg4Firmware.detectedString()} $line")
        // Only changes go to the report: this runs every 30 s.
        val charge = vehicle.chargingStatus to (latest?.charging == true)
        if (charge != reportedCharge) {
            reportedCharge = charge
            reportLog("MG4 $line")
        }
    }

    private fun log(message: String) {
        Log.i(TAG, "MG4 car: $message")
        reportLog("MG4 car: $message")
    }
}

/** Whether the MG4 is charging, as Apple Maps' isCharging wants it. */
internal object Mg4Charging {
    // The car's own charging-session values: 1 AC, 10 DC.
    private val CHARGING_STATUSES = setOf(1, 10)
    private const val CHARGING_POWER_KW = -0.3f

    /**
     * The car's charging status decides when it reports one: plugged in but stopped or finished is
     * not charging. Without it, falls back to the plug or power flowing into the pack.
     */
    fun isCharging(status: Int?, portConnected: Boolean?, batteryPowerKw: Float?): Boolean =
        if (status != null) status in CHARGING_STATUSES
        else portConnected == true || (batteryPowerKw?.let { it < CHARGING_POWER_KW } == true)
}
