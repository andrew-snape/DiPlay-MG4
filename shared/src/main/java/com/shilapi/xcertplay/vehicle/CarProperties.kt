package com.shilapi.xcertplay.vehicle

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.lang.reflect.Method

/**
 * Read-only access to the car's CarPropertyManager, through reflection because the Car API is
 * not in the app's compile classpath. A value counts only when the vehicle reports it as
 * available; a declared but unpublished property reads as null, not as zero.
 */
internal object CarProperties {
    private const val TAG = "DiPlay-MG4-Car"
    /** AAOS `AREA_GLOBAL`: the car-wide value for a property that has no per-seat or per-wheel area. */
    const val AREA_GLOBAL = 0x1000000
    private const val STATUS_AVAILABLE = 0

    @Volatile private var propertyManager: Any? = null
    @Volatile private var getProperty: Method? = null
    @Volatile private var connectRequested = false
    @Volatile private var failure: String? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            runCatching {
                val car = carHolder ?: return@runCatching
                val carClass = car.javaClass
                val serviceName = Class.forName("android.car.Car").getField("PROPERTY_SERVICE").get(null) as String
                val manager = carClass.getMethod("getCarManager", String::class.java).invoke(car, serviceName)
                    ?: error("no property manager")
                getProperty = manager.javaClass.getMethod(
                    "getProperty",
                    Class::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
                propertyManager = manager
                Log.i(TAG, "car property manager ready")
            }.onFailure {
                failure = it.javaClass.simpleName
                Log.w(TAG, "car property manager unavailable", it)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            propertyManager = null
            getProperty = null
            connectRequested = false
        }
    }

    @Volatile private var carHolder: Any? = null

    /** Creates the Car connection once; the property manager is ready when the car calls back. */
    @Synchronized
    fun connect(context: Context): String {
        if (propertyManager != null) return "ready"
        if (connectRequested) return "connecting"
        connectRequested = true
        runCatching {
            val car = Class.forName("android.car.Car")
                .getMethod("createCar", Context::class.java, ServiceConnection::class.java)
                .invoke(null, context.applicationContext, connection)
            carHolder = car
        }.onFailure {
            failure = it.javaClass.simpleName
            Log.w(TAG, "createCar failed", it)
        }
        return if (carHolder != null) "connecting" else "unavailable (${failure ?: "no car API"})"
    }

    fun isReady(): Boolean = propertyManager != null

    fun float(id: Int, area: Int = AREA_GLOBAL): Float? = read(id, area, Float::class.javaObjectType) as? Float

    fun int(id: Int, area: Int = AREA_GLOBAL): Int? = read(id, area, Int::class.javaObjectType) as? Int

    fun boolean(id: Int, area: Int = AREA_GLOBAL): Boolean? =
        read(id, area, Boolean::class.javaObjectType) as? Boolean

    private fun read(id: Int, area: Int, boxed: Class<*>): Any? {
        val manager = propertyManager ?: return null
        val method = getProperty ?: return null
        return runCatching {
            val holder = method.invoke(manager, boxed, id, area) ?: return@runCatching null
            val status = holder.javaClass.getMethod("getStatus").invoke(holder) as? Int
            if (status != STATUS_AVAILABLE) return@runCatching null
            holder.javaClass.getMethod("getValue").invoke(holder)
        }.getOrNull()
    }
}
