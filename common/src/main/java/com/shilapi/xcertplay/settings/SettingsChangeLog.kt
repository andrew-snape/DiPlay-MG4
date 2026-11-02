// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay.settings

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The last settings changes, kept across restarts for the diagnostic report. The session log
 * starts afresh with each CarPlay session, so a change made before connecting would be lost there.
 */
internal object SettingsChangeLog {
    private const val PREFS = "settings_change_log"
    private const val KEY = "lines"
    const val MAX_LINES = 40

    fun record(context: Context, row: SettingRow, value: String) = note(context, "${row.name} = $value")

    fun note(context: Context, message: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lines = append(prefs.getString(KEY, null), "$time  $message")
        prefs.edit().putString(KEY, lines).apply()
    }

    fun lines(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.lines()?.filter(String::isNotBlank).orEmpty()

    /** Adds [line] to the newline-separated [existing] and keeps the newest [MAX_LINES]. */
    fun append(existing: String?, line: String): String =
        (existing?.lines()?.filter(String::isNotBlank).orEmpty() + line).takeLast(MAX_LINES).joinToString("\n")
}
