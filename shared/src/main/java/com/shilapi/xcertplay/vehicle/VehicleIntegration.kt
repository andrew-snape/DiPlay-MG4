package com.shilapi.xcertplay.vehicle

import android.content.Context
import com.shilapi.xcertplay.transport.VehicleSpeedSource
import com.shilapi.xcertplay.transport.VehicleStatusProvider

/** The MG4 read path, kept apart from the CarPlay host. On other firmware there is no car data. */
object VehicleIntegration {
    fun isMg4(context: Context): Boolean = Mg4Vehicle.available(context)

    /** MG4 has a native read-only telemetry source, so EV routing is always on there. */
    fun vehicleStatusEnabled(context: Context): Boolean {
        if (!isMg4(context)) return false
        Mg4Vehicle.start(context)
        return true
    }

    fun onAppOpened(context: Context) {
        // ABRP was removed: delete its saved account (user token) once it is no longer used.
        runCatching { context.deleteSharedPreferences("diplay_abrp") }
        runCatching {
            if (isMg4(context)) {
                // Advertise the MG4 as an EV by default so Apple Maps can ask for live vehicle status.
                VehicleSettings.enableBatteryToIphoneByDefault(context)
                Mg4Vehicle.start(context)
            }
        }
    }

    fun batteryStatus(context: Context): VehicleStatusProvider = Mg4Vehicle.also { it.start(context) }

    /** MG4 reads speed directly, so it always goes with location. */
    fun wheelSpeedEnabled(context: Context): Boolean = isMg4(context)

    fun wheelSpeed(context: Context): VehicleSpeedSource = Mg4WheelSpeedSource

    /** Sends MG4 wheel-speed, gear and charging lines to [log] (the session log), or stops with null. */
    fun setReportLog(log: ((String) -> Unit)?) {
        Mg4WheelSpeedSource.reportLog = log ?: {}
        Mg4Vehicle.reportLog = log ?: {}
    }

    fun parked(context: Context): Boolean? = if (isMg4(context)) Mg4Vehicle.parked(context) else null

    /**
     * The Car data page: each signal read now and the source that answered. Makes binder and
     * car-property calls, so call it off the UI thread. Nothing here is a guess: a missing value
     * reads "unavailable".
     */
    fun carDataReport(context: Context): String {
        if (!Mg4Firmware.supported()) {
            return "Car data is read on SWI165 only.\nFirmware: ${Mg4Firmware.detectedString()}"
        }
        val hub = SaicBinder.connect(context.applicationContext)
        val properties = CarProperties.connect(context.applicationContext)
        val reading = Mg4CarReader(Mg4CarSignals).read()
        val gear = Mg4CarSignals.rawGear()
        val speed = Mg4CarSignals.speedMetersPerSecond()
        fun value(number: Any?, unit: String, signal: String): String {
            val source = Mg4CarSignals.sources[signal] ?: "not read"
            return if (number == null) "unavailable ($source)" else "$number$unit ($source)"
        }
        return buildString {
            appendLine("Firmware: ${Mg4Firmware.detectedString()}")
            appendLine("Vehicle hub: ${SaicBinder.hubState()} ($hub)")
            appendLine("Car properties: ${if (CarProperties.isReady()) "ready" else properties}")
            appendLine("Charge: ${value(reading.socPercent?.let { "%.0f".format(it) }, "%", "soc")}")
            appendLine("Range: ${value(reading.rangeKm, " km", "range")}")
            appendLine("Charging status: ${value(reading.chargingStatus, "", "chargeStatus")}")
            appendLine("Charge port connected: ${value(reading.chargePortConnected, "", "port")}")
            appendLine("Pack power: ${value(reading.batteryPowerKw?.let { "%.1f".format(it) }, " kW", "powerKw")}")
            appendLine("Energy: ${value(reading.batteryEnergyKwh?.let { "%.1f".format(it) }, " kWh", "energyKwh")}")
            appendLine("Capacity: ${value(reading.batteryCapacityKwh?.let { "%.1f".format(it) }, " kWh", "capacityKwh")}")
            appendLine("Gear: ${value(gear, "", "gear")} ${Mg4WheelSpeed.gearLabel(gear)}")
            append("Speed: ${value(speed?.let { "%.1f".format(it * 3.6f) }, " km/h", "speed")}")
        }
    }
}
