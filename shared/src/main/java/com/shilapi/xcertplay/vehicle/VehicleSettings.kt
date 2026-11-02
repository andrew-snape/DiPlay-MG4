package com.shilapi.xcertplay.vehicle

import android.content.Context
import com.shilapi.xcertplay.transport.EvChargingConnectors

/** The car's battery, charging and parked-video choices. Each applies on the next connection. */
object VehicleSettings {
    private const val PREFS = "diplay_vehicle"
    // Older builds kept these choices in the upstream file; they move across once (see prefs).
    private const val OLD_PREFS = "diplay_byd_outputs"
    private const val KEY_BATTERY_TO_IPHONE = "battery_to_iphone"
    private const val KEY_LOW_CHARGE_PERCENT = "low_charge_percent"
    private const val KEY_CHARGING_CONNECTORS = "charging_connectors"
    private const val KEY_VIDEO_WHILE_PARKED = "video_while_parked"
    const val DEFAULT_LOW_CHARGE_PERCENT = 20
    val lowChargePresets = listOf(10, 15, 20, 25, 30)

    /** Tell the iPhone the car's charge and range. */
    fun batteryToIphone(context: Context): Boolean = prefs(context).getBoolean(KEY_BATTERY_TO_IPHONE, false)

    fun setBatteryToIphone(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_BATTERY_TO_IPHONE, enabled).apply()

    /** Turn the battery on the first time; a saved choice wins. */
    fun enableBatteryToIphoneByDefault(context: Context) {
        val preferences = prefs(context)
        if (!preferences.contains(KEY_BATTERY_TO_IPHONE)) {
            preferences.edit().putBoolean(KEY_BATTERY_TO_IPHONE, true).apply()
        }
    }

    /** The charging inlets the iPhone is told about. */
    fun chargingConnectors(context: Context): EvChargingConnectors =
        prefs(context).getString(KEY_CHARGING_CONNECTORS, null)
            ?.let { saved -> EvChargingConnectors.entries.firstOrNull { it.name == saved } }
            ?: EvChargingConnectors.CCS2_TYPE2

    fun setChargingConnectors(context: Context, connectors: EvChargingConnectors) =
        prefs(context).edit().putString(KEY_CHARGING_CONNECTORS, connectors.name).apply()

    /** Offer iOS 27 video in car, played only while the car is in P. */
    fun videoWhileParked(context: Context): Boolean = prefs(context).getBoolean(KEY_VIDEO_WHILE_PARKED, false)

    fun setVideoWhileParked(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_VIDEO_WHILE_PARKED, enabled).apply()

    /** At or below this charge the iPhone gets the low-range warning. */
    fun lowChargePercent(context: Context): Int = prefs(context).getInt(KEY_LOW_CHARGE_PERCENT, DEFAULT_LOW_CHARGE_PERCENT)

    fun setLowChargePercent(context: Context, percent: Int) =
        prefs(context).edit().putInt(KEY_LOW_CHARGE_PERCENT, percent).apply()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs ->
        if (prefs.all.isNotEmpty()) return@also
        val old = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        if (old.all.isEmpty()) return@also
        val edit = prefs.edit()
        old.getBooleanOrNull(KEY_BATTERY_TO_IPHONE)?.let { edit.putBoolean(KEY_BATTERY_TO_IPHONE, it) }
        old.getBooleanOrNull(KEY_VIDEO_WHILE_PARKED)?.let { edit.putBoolean(KEY_VIDEO_WHILE_PARKED, it) }
        if (old.contains(KEY_LOW_CHARGE_PERCENT)) edit.putInt(KEY_LOW_CHARGE_PERCENT, old.getInt(KEY_LOW_CHARGE_PERCENT, DEFAULT_LOW_CHARGE_PERCENT))
        old.getString(KEY_CHARGING_CONNECTORS, null)?.let { edit.putString(KEY_CHARGING_CONNECTORS, it) }
        edit.commit()
        old.edit().clear().commit()
    }

    private fun android.content.SharedPreferences.getBooleanOrNull(key: String): Boolean? =
        if (contains(key)) getBoolean(key, false) else null
}
