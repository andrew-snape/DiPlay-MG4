package com.shilapi.xcertplay.airplay

import android.os.Debug
import java.io.File

/**
 * One-line system snapshot appended to a receive report when a stream stalls (read gap of a
 * second or more, or a long RTP gap). It helps tell a Wi-Fi outage from a busy head unit:
 * hotspot packet counters, signal, load, GC time. Every part fails closed as `unreadable`.
 */
internal object LinkSnapshot {
    private const val MIN_INTERVAL_MS = 10_000L
    private var lastAtMs = 0L

    @Synchronized
    fun describeIfDue(nowMs: Long = System.currentTimeMillis()): String? {
        if (nowMs - lastAtMs < MIN_INTERVAL_MS) return null
        lastAtMs = nowMs
        return describe()
    }

    fun describe(): String = listOf(
        "net" to { NetCounters.totals() },
        "wireless" to { File("/proc/net/wireless").readLines().drop(2).joinToString(";") { it.trim().replace(Regex("\\s+"), " ") }.ifEmpty { "none" } },
        "load" to { File("/proc/loadavg").readText().trim().split(" ").take(3).joinToString(" ") },
        "gcBlocking" to { "${Debug.getRuntimeStat("art.gc.blocking-gc-count")}/${Debug.getRuntimeStat("art.gc.blocking-gc-time")}ms" },
    ).joinToString(" ") { (name, read) -> "$name=" + runCatching(read).getOrElse { "unreadable" }.ifEmpty { "unreadable" } }
}
