package com.shilapi.xcertplay.media

import android.util.Log

/**
 * Pairs the car's "CarPlay phone" audio-policy hook: on, once, when a CarPlay call track starts;
 * off, once, when it stops. [invoke] makes the call and returns its result code, or null when it
 * could not be made. Pure logic, so the pairing can be tested.
 */
internal class CarPhoneModeHook(
    private val invoke: (available: Boolean) -> Int?,
    private val report: (String) -> Unit = {},
) {
    private var active = false

    @Synchronized
    fun set(available: Boolean, why: String) {
        if (available == active) return
        val result = invoke(available)
        // Only count a hook that went through, so a failed "on" is never followed by a stray "off".
        if (result != null) active = available
        report("Audio: car phone mode ${if (available) "on" else "off"} ($why) result=${result ?: "failed"}")
    }

    @Synchronized
    fun release(why: String) = set(false, why)
}

/**
 * The stock CarPlay audio library (libCarPlayPlatformAudio.so, R11) calls
 * `AudioSystem::setDeviceConnectionState(device=0x400, AVAILABLE, "", "carplay_source=phone")`
 * before it plays a call and again with UNAVAILABLE when the call track stops. It looks like a
 * vendor audio-policy hook that switches the car into CarPlay phone mode. Not yet tried in
 * the car.
 *
 * `AudioSystem` is hidden, so it is called by reflection. Android 9 has
 * `(int device, int state, String address, String name)`; later releases add a codec argument.
 * If the method is missing or refuses, the result is null and calls carry on as before.
 */
internal object Mg4CarPhoneMode {
    private const val TAG = "DiPlay-CarPhoneMode"
    const val DEVICE = 0x400
    const val STATE_UNAVAILABLE = 0
    const val STATE_AVAILABLE = 1
    const val NAME = "carplay_source=phone"

    fun hook(report: (String) -> Unit): CarPhoneModeHook = CarPhoneModeHook(::call, report)

    /** The return code of setDeviceConnectionState, or null when it could not be called. */
    fun call(available: Boolean): Int? = runCatching {
        val type = Class.forName("android.media.AudioSystem")
        val method = type.methods.firstOrNull {
            it.name == "setDeviceConnectionState" && it.parameterTypes.firstOrNull() == Int::class.javaPrimitiveType &&
                it.parameterTypes.size in 4..5
        } ?: error("setDeviceConnectionState not found")
        val state = if (available) STATE_AVAILABLE else STATE_UNAVAILABLE
        val args: Array<Any> = if (method.parameterTypes.size == 4) arrayOf(DEVICE, state, "", NAME)
        else arrayOf(DEVICE, state, "", NAME, 0)
        method.invoke(null, *args) as? Int
    }.onFailure { Log.w(TAG, "setDeviceConnectionState failed", it) }.getOrNull()
}
