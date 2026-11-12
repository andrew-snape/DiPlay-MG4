// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay.settings

import com.shilapi.xcertplay.Mg4FirmwareProfile

/**
 * Which settings the MG4 Settings screen shows, and where. Plain Kotlin, so the layout for each
 * firmware can be unit-tested. [Mg4SettingsPage] turns it into views and reads and saves values.
 */
internal enum class SettingsCategory { CONNECTION, DISPLAY, SOUND, CAR, GENERAL, HELP, ABOUT }

internal enum class RowKind {
    /** On/off switch. */
    SWITCH,
    /** Two or three short options shown inline. */
    SEGMENTED,
    /** Shows the current value; opens a list of options with a tick. */
    PICKER,
    /** Opens a page of more settings. */
    PAGE,
    /** Opens a dialog or the car's own settings; may show a value. */
    LINK,
    /** Does something straight away; shown as accent text. */
    ACTION,
    /** Read-only value. */
    INFO,
}

/**
 * Every row. [appliesOnReconnect] marks values the running CarPlay session doesn't pick up:
 * changing one while connected offers **Reconnect now**.
 */
internal enum class SettingRow(val kind: RowKind, val appliesOnReconnect: Boolean = false) {
    CONNECTION_TYPE(RowKind.PICKER, true),
    HOTSPOT_DETAILS(RowKind.LINK, true),
    IPHONE(RowKind.LINK),
    AUTO_CONNECT(RowKind.SWITCH),
    OPEN_AFTER_START(RowKind.SWITCH),
    WIFI_HIGH_PERFORMANCE(RowKind.SWITCH, true),
    WIFI_DIRECT_BAND(RowKind.PICKER, true),
    OPEN_CAR_WIFI(RowKind.LINK),

    CARPLAY_SIZE(RowKind.PICKER, true),
    REPORTED_SCREEN_SIZE(RowKind.PICKER, true),
    RIGHT_HAND_DRIVE(RowKind.SWITCH, true),
    FULL_SCREEN(RowKind.SWITCH, true),
    RESOLUTION(RowKind.SEGMENTED, true),
    FRAME_RATE(RowKind.SEGMENTED, true),
    EFFICIENT_VIDEO(RowKind.SWITCH, true),
    DISPLAY_ADVANCED(RowKind.PAGE),
    OVERLAY_RENDERER(RowKind.SWITCH, true),
    COLOUR_RANGE(RowKind.PICKER, true),
    SQUARE_CORNERS(RowKind.SWITCH, true),

    WIFI_AUDIO(RowKind.SWITCH, true),
    CALLS_OVER_CARPLAY(RowKind.SWITCH, true),
    SHOW_CALLS_TO_CAR(RowKind.SWITCH, true),
    TAG_MUSIC_CARPLAY(RowKind.SWITCH, true),
    CAR_PHONE_MODE(RowKind.SWITCH, true),
    NARROW_BAND_CALLS(RowKind.SWITCH, true),
    MUSIC_BUFFER(RowKind.SEGMENTED, true),
    MICROPHONE(RowKind.PICKER, true),
    SIRI_MIC_PCM(RowKind.SWITCH, true),
    MIC_RECORDING(RowKind.SWITCH),
    SOUND_ADVANCED(RowKind.PAGE),
    NAVIGATION_CHANNEL(RowKind.LINK, true),

    CLUSTER_DIRECTIONS(RowKind.SWITCH, true),
    CAR_MFI_CHIP(RowKind.SWITCH, true),
    // Turning location on asks for permission and reconnects by itself, as before.
    SEND_LOCATION(RowKind.SWITCH),
    VIDEO_WHILE_PARKED(RowKind.SWITCH, true),
    SUNSET_DARK_MODE(RowKind.SWITCH, true),
    CAR_BATTERY(RowKind.SWITCH, true),
    CHARGING_CONNECTORS(RowKind.PICKER, true),
    LOW_CHARGE_WARNING(RowKind.PICKER),


    SOFTWARE_UPDATE(RowKind.LINK),
    APP_PERMISSIONS(RowKind.LINK),
    BLUETOOTH(RowKind.LINK),

    SAVE_REPORT(RowKind.ACTION),
    DOWNLOAD_REPORT(RowKind.ACTION),
    REPORT_LOCATION(RowKind.ACTION),
    WIRELESS_HELP(RowKind.LINK),

    VERSION(RowKind.INFO),
    CREDITS(RowKind.INFO),
}

internal enum class ConnectionChoice { CAR_HOTSPOT, WIFI_DIRECT }

/** A rounded group of rows. [key] picks its heading and footer. A group may have no rows. */
internal data class SettingsGroup(val key: String, val rows: List<SettingRow>)

internal data class SettingsEnvironment(
    val generation: Mg4FirmwareProfile.Generation,
    val sdkInt: Int,
    val connection: ConnectionChoice,
)

internal object SettingsLayout {
    val categories: List<SettingsCategory> = SettingsCategory.entries

    fun groups(category: SettingsCategory, env: SettingsEnvironment): List<SettingsGroup> = when (category) {
        SettingsCategory.CONNECTION -> listOfNotNull(
            SettingsGroup("connection", listOfNotNull(
                SettingRow.CONNECTION_TYPE,
                SettingRow.HOTSPOT_DETAILS.takeIf { env.connection == ConnectionChoice.CAR_HOTSPOT },
                SettingRow.IPHONE,
            )),
            SettingsGroup("automatic", listOf(SettingRow.AUTO_CONNECT, SettingRow.OPEN_AFTER_START)),
            SettingsGroup("wifi", listOfNotNull(
                SettingRow.WIFI_HIGH_PERFORMANCE,
                // The band choice only reaches the Wi-Fi Direct group on Android 9 and older.
                SettingRow.WIFI_DIRECT_BAND.takeIf { env.connection == ConnectionChoice.WIFI_DIRECT && env.sdkInt < 29 },
                SettingRow.OPEN_CAR_WIFI,
            )),
        )
        SettingsCategory.DISPLAY -> listOf(
            SettingsGroup("screen", listOfNotNull(
                SettingRow.CARPLAY_SIZE,
                SettingRow.REPORTED_SCREEN_SIZE.takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
                SettingRow.RIGHT_HAND_DRIVE,
                SettingRow.FULL_SCREEN,
            )),
            SettingsGroup("picture", listOf(SettingRow.RESOLUTION, SettingRow.FRAME_RATE, SettingRow.EFFICIENT_VIDEO)),
            SettingsGroup("advanced", listOf(SettingRow.DISPLAY_ADVANCED)),
        )
        SettingsCategory.SOUND -> listOf(
            SettingsGroup("wifi_audio", listOf(SettingRow.WIFI_AUDIO)),
            SettingsGroup("calls", listOfNotNull(
                SettingRow.CALLS_OVER_CARPLAY,
                // Telecom mirroring and the music source tag copy the stock app's SAIC audio paths.
                SettingRow.SHOW_CALLS_TO_CAR.takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
                SettingRow.TAG_MUSIC_CARPLAY.takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
                SettingRow.CAR_PHONE_MODE.takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
                SettingRow.NARROW_BAND_CALLS.takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
            )),
            SettingsGroup("buffer", listOf(SettingRow.MUSIC_BUFFER)),
            SettingsGroup("microphone", listOf(SettingRow.MICROPHONE, SettingRow.SIRI_MIC_PCM, SettingRow.MIC_RECORDING)),
            SettingsGroup("advanced", listOf(SettingRow.SOUND_ADVANCED)),
        )
        SettingsCategory.CAR -> listOfNotNull(
            SettingsGroup("cluster", listOf(SettingRow.CLUSTER_DIRECTIONS))
                .takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
            SettingsGroup("location", listOf(SettingRow.SEND_LOCATION)),
            SettingsGroup("parked", listOf(SettingRow.VIDEO_WHILE_PARKED)),
            SettingsGroup("appearance", listOf(SettingRow.SUNSET_DARK_MODE)),
            SettingsGroup("battery", listOf(SettingRow.CAR_BATTERY, SettingRow.CHARGING_CONNECTORS, SettingRow.LOW_CHARGE_WARNING)),
            SettingsGroup("mfi", listOf(SettingRow.CAR_MFI_CHIP))
                .takeIf { env.generation == Mg4FirmwareProfile.Generation.SWI165 },
        )
        SettingsCategory.GENERAL -> listOf(
            SettingsGroup("general", listOf(SettingRow.SOFTWARE_UPDATE)),
            SettingsGroup("system", listOf(SettingRow.APP_PERMISSIONS, SettingRow.BLUETOOTH)),
        )
        SettingsCategory.HELP -> listOf(
            SettingsGroup("diagnostics", listOf(SettingRow.SAVE_REPORT, SettingRow.DOWNLOAD_REPORT, SettingRow.REPORT_LOCATION)),
            SettingsGroup("help", listOf(SettingRow.WIRELESS_HELP)),
        )
        SettingsCategory.ABOUT -> listOf(
            SettingsGroup("about", listOf(SettingRow.VERSION, SettingRow.CREDITS)),
            SettingsGroup("licences", emptyList()),
        )
    }

    /** The rows on a [RowKind.PAGE] row's own page. */
    fun page(row: SettingRow): List<SettingsGroup> = when (row) {
        SettingRow.DISPLAY_ADVANCED -> listOf(
            SettingsGroup("overlay", listOf(SettingRow.OVERLAY_RENDERER)),
            SettingsGroup("colour", listOf(SettingRow.COLOUR_RANGE)),
            SettingsGroup("corners", listOf(SettingRow.SQUARE_CORNERS)),
        )
        SettingRow.SOUND_ADVANCED -> listOf(
            SettingsGroup("navigation", listOf(SettingRow.NAVIGATION_CHANNEL)),
        )
        else -> emptyList()
    }

    /** Every row the screen can show for [env], including those on their own pages. */
    fun allRows(env: SettingsEnvironment): List<SettingRow> = categories.flatMap { category ->
        groups(category, env).flatMap { group ->
            group.rows.flatMap { row -> listOf(row) + page(row).flatMap(SettingsGroup::rows) }
        }
    }
}
