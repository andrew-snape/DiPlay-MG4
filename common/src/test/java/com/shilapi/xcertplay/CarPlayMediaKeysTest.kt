package com.shilapi.xcertplay

import com.shilapi.xcertplay.CarPlayMediaKeys.RawKeyAction
import com.shilapi.xcertplay.CarPlayMediaKeys.SiriMuteAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// CarPlayMediaKeys creates a main-looper Handler when it loads.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CarPlayMediaKeysTest {
    @Test
    fun heldVoiceKeyOpensSiriWhileHeldAndNotAgainOnRelease() {
        // The car's sequence for a held voice key on 2026-11-03: down, down+long, then up.
        assertNull(CarPlayMediaKeys.rawKeyAction(287, down = true, longPress = false, callsOverCarPlay = false))
        assertEquals(RawKeyAction.SIRI, CarPlayMediaKeys.rawKeyAction(287, down = true, longPress = true, callsOverCarPlay = false))
        assertNull(CarPlayMediaKeys.rawKeyAction(287, down = true, longPress = true, callsOverCarPlay = false, voiceHeld = true))
        assertNull(CarPlayMediaKeys.rawKeyAction(287, down = false, longPress = false, callsOverCarPlay = false, voiceHeld = true))
    }

    @Test
    fun shortVoicePressOpensSiriOnRelease() {
        assertNull(CarPlayMediaKeys.rawKeyAction(287, down = true, longPress = false, callsOverCarPlay = false))
        assertEquals(RawKeyAction.SIRI, CarPlayMediaKeys.rawKeyAction(287, down = false, longPress = false, callsOverCarPlay = false))
    }

    @Test
    fun phoneKeyDrivesTheCallOnlyWhenCallsAreCarPlayOnly() {
        assertEquals(RawKeyAction.CALL_HOOK, CarPlayMediaKeys.rawKeyAction(291, down = false, longPress = false, callsOverCarPlay = true))
        assertNull(CarPlayMediaKeys.rawKeyAction(291, down = false, longPress = false, callsOverCarPlay = false))
        assertNull(CarPlayMediaKeys.rawKeyAction(291, down = false, longPress = true, callsOverCarPlay = true))
    }

    @Test
    fun otherKeysDoNothing() {
        assertNull(CarPlayMediaKeys.rawKeyAction(299, down = false, longPress = false, callsOverCarPlay = true))
        assertNull(CarPlayMediaKeys.rawKeyAction(286, down = false, longPress = false, callsOverCarPlay = true))
    }

    @Test
    fun muteKeyMutesSiriOnlyWhileSiriIsActiveAndNextPressOrVolumeUpRestores() {
        // Not active: the mute key does nothing here.
        assertNull(CarPlayMediaKeys.siriMuteAction(301, down = false, longPress = false, siriActive = false, siriMuted = false))
        // Active: a short release mutes; the press itself does not.
        assertNull(CarPlayMediaKeys.siriMuteAction(301, down = true, longPress = false, siriActive = true, siriMuted = false))
        assertEquals(SiriMuteAction.MUTE, CarPlayMediaKeys.siriMuteAction(301, down = false, longPress = false, siriActive = true, siriMuted = false))
        assertNull(CarPlayMediaKeys.siriMuteAction(301, down = false, longPress = true, siriActive = true, siriMuted = false))
        // Muted: the next press restores, even if Siri has just ended; so does volume up.
        assertEquals(SiriMuteAction.RESTORE, CarPlayMediaKeys.siriMuteAction(301, down = false, longPress = false, siriActive = false, siriMuted = true))
        assertEquals(SiriMuteAction.RESTORE, CarPlayMediaKeys.siriMuteAction(297, down = true, longPress = false, siriActive = true, siriMuted = true))
        assertNull(CarPlayMediaKeys.siriMuteAction(297, down = true, longPress = false, siriActive = true, siriMuted = false))
        assertNull(CarPlayMediaKeys.siriMuteAction(298, down = true, longPress = false, siriActive = true, siriMuted = true))
    }
}
