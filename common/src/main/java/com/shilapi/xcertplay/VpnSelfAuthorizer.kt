package com.shilapi.xcertplay

import android.content.Context
import android.net.ConnectivityManager
import android.os.Process

/**
 * USB CarPlay runs over a VpnService, which normally needs the user to accept Android's
 * `com.android.vpndialogs` ConfirmDialog. The MG4 head unit (SWI165, Android 9) ships without that
 * app, so launching it crashed (car test 2026-11-03). Choosing USB CarPlay is the owner's consent
 * here; a platform-signed build running as the system uid holds CONTROL_VPN, so it records that
 * consent the way the dialog would: `prepareVpn` and `setVpnPackageAuthorization` on the hidden
 * IConnectivityManager, for this package only. Returns whether that worked.
 */
internal object VpnSelfAuthorizer {
    fun authorize(context: Context, log: (String) -> Unit): Boolean = runCatching {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: error("no ConnectivityManager")
        val service = ConnectivityManager::class.java.getDeclaredField("mService")
            .apply { isAccessible = true }
            .get(connectivity) ?: error("no IConnectivityManager")
        val userId = runCatching {
            android.os.UserHandle::class.java.getMethod("getIdentifier").invoke(Process.myUserHandle()) as Int
        }.getOrDefault(0)
        val type = service.javaClass
        val prepared = type.getMethod("prepareVpn", String::class.java, String::class.java, Int::class.javaPrimitiveType)
            .invoke(service, null, context.packageName, userId) as Boolean
        type.getMethod("setVpnPackageAuthorization", String::class.java, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            .invoke(service, context.packageName, userId, true)
        log("USB VPN: authorised for this app without the system dialog prepared=$prepared")
        true
    }.getOrElse { error ->
        val cause = (error as? java.lang.reflect.InvocationTargetException)?.targetException ?: error
        log("USB VPN: self-authorisation failed: ${cause.javaClass.simpleName}: ${cause.message}")
        false
    }
}
