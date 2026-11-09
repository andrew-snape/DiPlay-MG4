package com.shilapi.xcertplay.vehicle

import android.util.Log

/**
 * The firmware gate for MG4 car reads. Only SWI165 is supported; any other firmware, or no
 * firmware at all, keeps every car read off (fail closed).
 */
internal object Mg4Firmware {
    private const val TAG = "DiPlay-MG4"
    const val PROPERTY = "ro.build.mt2712.version"
    private const val SUPPORTED_PREFIX = "SWI165"

    fun isSwi165(firmware: String?): Boolean = firmware?.trim()?.startsWith(SUPPORTED_PREFIX) == true

    /** The running head unit's firmware string, or null when the property is absent. */
    fun read(): String? = runCatching {
        val properties = Class.forName("android.os.SystemProperties")
        val get = properties.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, PROPERTY, "") as? String)?.takeIf { it.isNotBlank() }
    }.onFailure { Log.w(TAG, "Could not read $PROPERTY", it) }.getOrNull()

    /** Read once per process. */
    private val detected: String? by lazy { read() }

    fun supported(): Boolean = isSwi165(detected)

    fun detectedString(): String = detected ?: "firmware unknown"
}
