package com.shilapi.xcertplay

import android.content.Context
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class Mg4ScreenAndCallSettingsTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun screenSizeIsAutomaticByDefault() {
        assertFalse(AirPlayPersistence.loadStockScreenSize(context))
    }

    @Test fun narrowBandCallsAreOffByDefault() {
        assertFalse(AirPlayPersistence.loadNarrowBandCalls(context))
    }
}
