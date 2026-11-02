package com.shilapi.xcertplay

import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DayNightProbeTest {
    @Test
    fun picksDayNightNamesOnly() {
        assertTrue(DayNightProbe.isCandidate("ui_night_mode"))
        assertTrue(DayNightProbe.isCandidate("saic_day_night_mode"))
        assertTrue(DayNightProbe.isCandidate("headlamp_state"))
        assertTrue(DayNightProbe.isCandidate("screen_brightness_dim"))
        assertFalse(DayNightProbe.isCandidate("screen_off_timeout"))
        assertFalse(DayNightProbe.isCandidate("auto_time_zone_delay"))
        assertFalse(DayNightProbe.isCandidate("airplane_mode_on"))
    }

    @Test
    fun snapshotReportsNightModeAndBrightness() {
        // Robolectric's settings provider cannot list a whole table, so the table scan is checked in the car.
        val context = RuntimeEnvironment.getApplication()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 120)
        val line = DayNightProbe.snapshot(context)
        assertTrue(line, line.startsWith("uiNight=false uiModeManager="))
        assertTrue(line, line.contains("brightness=120"))
    }
}
