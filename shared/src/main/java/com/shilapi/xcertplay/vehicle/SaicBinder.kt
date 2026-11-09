package com.shilapi.xcertplay.vehicle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.util.Log

/**
 * Binds the SAIC vehicle hub and calls its AIDL services. Every call is read-only here and
 * returns null when the service is missing, refuses, throws, or answers with an unexpected
 * shape: a null is never turned into a guessed value.
 */
internal object SaicBinder {
    private const val TAG = "DiPlay-MG4-SAIC"
    private const val HUB_PACKAGE = "com.saicmotor.service.vehicle"
    private const val HUB_ACTION = "com.saicmotor.service.vehicle.VehicleService"
    private const val HUB_DESCRIPTOR = "com.saicmotor.sdk.vehiclesettings.IHubService"
    private const val TX_GET_SERVICE = 1

    @Volatile private var hub: IBinder? = null
    @Volatile private var bindRequested = false
    private val resolved = HashMap<String, IBinder>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(this@SaicBinder) {
                hub = service
                resolved.clear()
            }
            Log.i(TAG, "vehicle hub connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(this@SaicBinder) {
                hub = null
                bindRequested = false
                resolved.clear()
            }
            Log.w(TAG, "vehicle hub disconnected")
        }
    }

    /** Starts the asynchronous bind once; again only after the hub has disconnected. */
    @Synchronized
    fun connect(context: Context): String {
        if (hub?.isBinderAlive == true) return "connected"
        if (bindRequested) return "bind pending"
        bindRequested = runCatching {
            context.applicationContext.bindService(
                Intent(HUB_ACTION).setPackage(HUB_PACKAGE),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }.getOrDefault(false)
        return if (bindRequested) "bind requested" else "bind refused"
    }

    /** The binder of one sub-service (for example `vehiclecharging`), resolved from the hub. */
    @Synchronized
    fun service(name: String): IBinder? {
        val live = hub?.takeIf { it.isBinderAlive }
        if (live == null) {
            resolved.clear()
            return null
        }
        resolved[name]?.takeIf { it.isBinderAlive }?.let { return it }
        val sub = callBinder(live, HUB_DESCRIPTOR, TX_GET_SERVICE, name) ?: return null
        resolved[name] = sub
        return sub
    }

    /** Hub state for the diagnostic log and the Car data page. */
    fun hubState(): String = when {
        hub?.isBinderAlive == true -> "connected"
        bindRequested -> "bind pending"
        else -> "not connected"
    }

    fun callInt(binder: IBinder?, descriptor: String, code: Int, vararg args: Any?): Int? =
        transact(binder, descriptor, code, args) { it.readInt() }

    fun callBoolean(binder: IBinder?, descriptor: String, code: Int, vararg args: Any?): Boolean? =
        transact(binder, descriptor, code, args) { it.readInt() != 0 }

    fun callFloat(binder: IBinder?, descriptor: String, code: Int, vararg args: Any?): Float? =
        transact(binder, descriptor, code, args) { it.readFloat() }

    fun callBinder(binder: IBinder?, descriptor: String, code: Int, vararg args: Any?): IBinder? =
        transact(binder, descriptor, code, args) { it.readStrongBinder() }

    /** The AIDL calling convention: interface token, arguments, transact, then readException. */
    private fun <T> transact(
        binder: IBinder?,
        descriptor: String,
        code: Int,
        args: Array<out Any?>,
        readReply: (Parcel) -> T,
    ): T? {
        val target = binder?.takeIf { it.isBinderAlive } ?: return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(descriptor)
            for (arg in args) {
                when (arg) {
                    is Int -> data.writeInt(arg)
                    is Boolean -> data.writeInt(if (arg) 1 else 0)
                    is Float -> data.writeFloat(arg)
                    is String -> data.writeString(arg)
                    else -> return null
                }
            }
            if (!target.transact(code, data, reply, 0)) return null
            reply.readException()
            return readReply(reply)
        } catch (error: Exception) {
            Log.d(TAG, "$descriptor#$code: ${error.message}")
            return null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}
