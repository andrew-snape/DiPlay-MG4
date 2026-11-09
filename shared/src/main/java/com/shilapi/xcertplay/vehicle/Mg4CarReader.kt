package com.shilapi.xcertplay.vehicle

import java.util.concurrent.ConcurrentHashMap

/** Raw car signals, each null when the car does not stand behind a value. */
internal interface Mg4Signals {
    fun socPercent(): Float?
    fun rangeKm(): Float?
    fun chargingStatus(): Int?
    fun chargePortConnected(): Boolean?
    /** Pack power in kW; negative while charging (the car reports positive for charge). */
    fun batteryPowerKw(): Float?
    fun batteryEnergyKwh(): Float?
    fun batteryCapacityKwh(): Float?
    /** Gear code from vehiclecondition: 1 P, 2 R, 3 N, 4 D. */
    fun rawGear(): Int?
    /** Speed magnitude in metres per second. */
    fun speedMetersPerSecond(): Float?
}

/** A coherent reading of the car. Every field is null when unreadable. */
internal data class Mg4Reading(
    val socPercent: Float?,
    val rangeKm: Int?,
    val chargingStatus: Int?,
    val chargePortConnected: Boolean?,
    val batteryPowerKw: Float?,
    val batteryEnergyKwh: Float?,
    val batteryCapacityKwh: Float?,
)

/** Applies sanity bounds to the raw signals. Anything out of range becomes null, never a guess. */
internal class Mg4CarReader(private val signals: Mg4Signals) {
    fun read(): Mg4Reading = Mg4Reading(
        socPercent = signals.socPercent()?.takeIf { it.isFinite() && it in 0f..100f },
        rangeKm = signals.rangeKm()?.takeIf { it.isFinite() && it >= 0f }?.let { Math.round(it) },
        chargingStatus = signals.chargingStatus()?.takeIf { it >= 0 },
        chargePortConnected = signals.chargePortConnected(),
        batteryPowerKw = signals.batteryPowerKw()?.takeIf { it.isFinite() && kotlin.math.abs(it) <= 1_000f },
        batteryEnergyKwh = signals.batteryEnergyKwh()?.takeIf { it.isFinite() && it >= 0f },
        batteryCapacityKwh = signals.batteryCapacityKwh()?.takeIf { it.isFinite() && it > 0f },
    )
}

/**
 * The real car. Each signal tries its sources in order and records which one answered, so the
 * Car data page and the report can say where a number came from.
 */
internal object Mg4CarSignals : Mg4Signals {
    private const val CHARGING_DESCRIPTOR = "com.saicmotor.sdk.vehiclesettings.IVehicleChargingService"
    private const val CONDITION_DESCRIPTOR = "com.saicmotor.sdk.vehiclesettings.IVehicleConditionService"
    private const val TX_CHARGING_SOC = 3
    private const val TX_CHARGING_RANGE_KM = 4
    private const val TX_CHARGING_STATUS = 9
    private const val TX_CONDITION_GEAR = 5

    private const val PROP_SPEED = 0x11600207
    private const val PROP_RANGE_METRES = 0x11600308
    private const val PROP_CHARGE_PORT = 0x1120030B
    private const val PROP_POWER_MILLIWATTS = 0x1160030C
    private const val PROP_BATTERY_LEVEL_WH = 0x11600309
    private const val PROP_CAPACITY_WH = 0x11600106
    private const val PROP_VENDOR_CHARGE_STATUS = 0x2140F409

    /** Which source answered for each signal on the last read, for the Car data page and log. */
    val sources = ConcurrentHashMap<String, String>()

    private fun note(signal: String, source: String?) {
        sources[signal] = source ?: "none"
    }

    private fun charging() = SaicBinder.service("vehiclecharging")
    private fun condition() = SaicBinder.service("vehiclecondition")

    override fun socPercent(): Float? {
        val value = SaicBinder.callFloat(charging(), CHARGING_DESCRIPTOR, TX_CHARGING_SOC)
        note("soc", if (value != null) "vehiclecharging" else null)
        return value
    }

    override fun rangeKm(): Float? {
        SaicBinder.callInt(charging(), CHARGING_DESCRIPTOR, TX_CHARGING_RANGE_KM)?.let {
            note("range", "vehiclecharging")
            return it.toFloat()
        }
        CarProperties.float(PROP_RANGE_METRES)?.let {
            note("range", "property")
            return it / 1_000f
        }
        note("range", null)
        return null
    }

    override fun chargingStatus(): Int? {
        SaicBinder.callInt(charging(), CHARGING_DESCRIPTOR, TX_CHARGING_STATUS)?.let {
            note("chargeStatus", "vehiclecharging")
            return it
        }
        CarProperties.int(PROP_VENDOR_CHARGE_STATUS)?.let {
            note("chargeStatus", "property")
            return it
        }
        note("chargeStatus", null)
        return null
    }

    override fun chargePortConnected(): Boolean? {
        val value = CarProperties.boolean(PROP_CHARGE_PORT)
        note("port", if (value != null) "property" else null)
        return value
    }

    override fun batteryPowerKw(): Float? {
        val value = CarProperties.float(PROP_POWER_MILLIWATTS)?.div(-1_000_000f)
        note("powerKw", if (value != null) "property" else null)
        return value
    }

    override fun batteryEnergyKwh(): Float? {
        val value = CarProperties.float(PROP_BATTERY_LEVEL_WH)?.div(1_000f)
        note("energyKwh", if (value != null) "property" else null)
        return value
    }

    override fun batteryCapacityKwh(): Float? {
        val value = CarProperties.float(PROP_CAPACITY_WH)?.div(1_000f)
        note("capacityKwh", if (value != null) "property" else null)
        return value
    }

    override fun rawGear(): Int? {
        val value = SaicBinder.callInt(condition(), CONDITION_DESCRIPTOR, TX_CONDITION_GEAR)?.takeIf { it > 0 }
        note("gear", if (value != null) "vehiclecondition" else null)
        return value
    }

    override fun speedMetersPerSecond(): Float? {
        // SWI165 reports m/s (AAOS unit). The sign is reverse, so the magnitude is what is used.
        val value = CarProperties.float(PROP_SPEED)?.let { kotlin.math.abs(it) }
        note("speed", if (value != null) "property" else null)
        return value
    }
}
