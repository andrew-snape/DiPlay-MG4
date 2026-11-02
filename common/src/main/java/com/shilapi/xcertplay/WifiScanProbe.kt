package com.shilapi.xcertplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.SystemClock

/**
 * Logs each Wi-Fi scan the head unit finishes, to test whether the stalls about every 160 s on the
 * car hotspot (test plan F8-F10) line up with Android 9's background scans. Read-only: it never
 * starts a scan itself.
 */
internal class WifiScanProbe(
    private val context: Context,
    private val handler: Handler,
    private val log: (String) -> Unit,
) {
    private var registered = false
    private var lastScanElapsed = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = SystemClock.elapsedRealtime()
            val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            log(line(updated, if (lastScanElapsed == 0L) null else now - lastScanElapsed, resultCount()))
            lastScanElapsed = now
        }
    }

    fun start() {
        if (registered) return
        registered = runCatching {
            context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), null, handler)
        }.isSuccess
        val wifi = context.getSystemService(WifiManager::class.java)
        log(
            "Wi-Fi scan probe: listening=$registered wifiEnabled=${runCatching { wifi?.isWifiEnabled }.getOrNull()} " +
                "scanAlwaysAvailable=${runCatching { wifi?.isScanAlwaysAvailable }.getOrNull()}",
        )
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    @Suppress("MissingPermission")
    private fun resultCount(): Int? =
        runCatching { context.getSystemService(WifiManager::class.java)?.scanResults?.size }.getOrNull()

    companion object {
        internal fun line(updated: Boolean, sinceLastMillis: Long?, results: Int?): String =
            "Wi-Fi scan done updated=$updated results=${results ?: "?"} " +
                "sinceLast=${sinceLastMillis?.let { "${it / 1000}s" } ?: "first"}"
    }
}
