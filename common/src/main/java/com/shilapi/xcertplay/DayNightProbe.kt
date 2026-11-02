package com.shilapi.xcertplay

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings

/**
 * Finds the MG4's day/night signal. CarPlay's dark mode follows Android's night mode, but no car
 * headlight or day/night signal is known on SWI165. This logs Android's night mode, the screen
 * brightness and every system setting whose name suggests day/night, then logs each change of one
 * while CarPlay runs, so a drive into dusk (or switching the headlights on) shows which one moves.
 * Read-only.
 */
internal class DayNightProbe(
    private val context: Context,
    private val handler: Handler,
    private val log: (String) -> Unit,
) {
    private val observers = mutableListOf<ContentObserver>()

    fun start() {
        if (observers.isNotEmpty()) return
        log("Day/night probe: ${snapshot(context)}")
        for (table in TABLES) {
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    val name = uri?.lastPathSegment ?: return
                    if (!isCandidate(name)) return
                    log("Day/night probe: ${table.name}.$name=${read(context, table, name)} uiNight=${uiNight(context)}")
                }
            }
            runCatching { context.contentResolver.registerContentObserver(table.uri, true, observer) }
                .onSuccess { observers += observer }
        }
    }

    fun stop() {
        observers.forEach { runCatching { context.contentResolver.unregisterContentObserver(it) } }
        observers.clear()
    }

    internal enum class Table(val uri: Uri) {
        SYSTEM(Settings.System.CONTENT_URI),
        SECURE(Settings.Secure.CONTENT_URI),
        GLOBAL(Settings.Global.CONTENT_URI),
    }

    companion object {
        private val TABLES = Table.entries
        private val CANDIDATE = Regex("night|day|dark|light|lamp|illum|theme|dim|backlight|dusk|sun", RegexOption.IGNORE_CASE)
        private val NOT_CANDIDATE = Regex("delay|display_density|daylight_saving|highlight|flight", RegexOption.IGNORE_CASE)

        internal fun isCandidate(name: String): Boolean = CANDIDATE.containsMatchIn(name) && !NOT_CANDIDATE.containsMatchIn(name)

        /** One line for the session log and the diagnostic report. */
        fun snapshot(context: Context): String = buildString {
            append("uiNight=").append(uiNight(context))
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            append(" uiModeManager=").append(runCatching { uiModeManager?.nightMode }.getOrNull())
            append(" brightness=").append(read(context, Table.SYSTEM, Settings.System.SCREEN_BRIGHTNESS))
            for (table in TABLES) {
                for ((name, value) in candidates(context, table)) append(' ').append(table.name).append('.').append(name).append('=').append(value)
            }
        }

        private fun uiNight(context: Context): Boolean = isDarkMode(context.resources.configuration.uiMode)

        private fun read(context: Context, table: Table, name: String): String? = runCatching {
            when (table) {
                Table.SYSTEM -> Settings.System.getString(context.contentResolver, name)
                Table.SECURE -> Settings.Secure.getString(context.contentResolver, name)
                Table.GLOBAL -> Settings.Global.getString(context.contentResolver, name)
            }
        }.getOrNull()

        private fun candidates(context: Context, table: Table): List<Pair<String, String?>> = runCatching {
            context.contentResolver.query(table.uri, arrayOf("name", "value"), null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0) ?: continue
                        if (isCandidate(name)) add(name to cursor.getString(1))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}
