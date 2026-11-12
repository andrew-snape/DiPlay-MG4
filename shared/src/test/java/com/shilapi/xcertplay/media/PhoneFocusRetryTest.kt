package com.shilapi.xcertplay.media

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneFocusRetryTest {
    private val granted = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    private val failed = AudioManager.AUDIOFOCUS_REQUEST_FAILED

    @Test
    fun retriesOnlyRefusedPhoneFocusWhenEnabled() {
        assertTrue(PhoneFocusRetry.shouldRetry(true, AudioChannel.PHONE, failed))
        assertFalse(PhoneFocusRetry.shouldRetry(true, AudioChannel.PHONE, granted))
        assertFalse(PhoneFocusRetry.shouldRetry(true, AudioChannel.PHONE, null))
        assertFalse(PhoneFocusRetry.shouldRetry(false, AudioChannel.PHONE, failed))
        assertFalse(PhoneFocusRetry.shouldRetry(true, AudioChannel.MEDIA, failed))
        assertFalse(PhoneFocusRetry.shouldRetry(true, AudioChannel.ASSISTANT, failed))
    }

    @Test
    fun waitsTwoHundredMilliseconds() {
        assertEquals(200L, PhoneFocusRetry.DELAY_MILLIS)
    }

    @Test
    fun carSourceTagsMatchTheStockApp() {
        assertEquals(50, Mg4CarAudioSource.CARPLAY_MEDIA)
        assertEquals(52, Mg4CarAudioSource.CARPLAY_NAVIGATION)
        assertEquals(53, Mg4CarAudioSource.CARPLAY_PHONE)
    }
}
