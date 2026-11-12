package com.shilapi.xcertplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Mg4CarIntegrationTest {
    @Test
    fun nativeBluetoothCallsRingingAndOffhookAreActive() {
        assertEquals(true, VendorCallStatus.nativeCallActive(state = 4, type = 0))
        assertEquals(true, VendorCallStatus.nativeCallActive(state = 5, type = 0))
        assertEquals(false, VendorCallStatus.nativeCallActive(state = 7, type = 0))
        assertEquals(false, VendorCallStatus.nativeCallActive(state = 0, type = 0))
    }

    @Test
    fun carPlayAndOtherCallTypesNeverTouchFocus() {
        assertNull(VendorCallStatus.nativeCallActive(state = 5, type = 2))
        assertNull(VendorCallStatus.nativeCallActive(state = 4, type = 9))
    }

    @Test
    fun describesStatesForTheLog() {
        assertEquals("native Bluetooth ringing", VendorCallStatus.describe(4, 0))
        assertEquals("CarPlay offhook", VendorCallStatus.describe(5, 2))
        assertEquals("CarPlay idle", VendorCallStatus.describe(7, 2))
        assertEquals(0x60000, VendorCallStatus.LISTEN_MASK)
    }

    @Test
    fun displayComingBackAsksForAKeyFrameOnlyAfterBeingOff() {
        assertTrue(DisplayReport.displayOff(true))
        assertFalse(DisplayReport.displayOff(false))
        assertTrue(DisplayReport.shouldRequestKeyFrame(previousOff = true, nowOff = false))
        assertFalse(DisplayReport.shouldRequestKeyFrame(previousOff = null, nowOff = false))
        assertFalse(DisplayReport.shouldRequestKeyFrame(previousOff = false, nowOff = false))
        assertFalse(DisplayReport.shouldRequestKeyFrame(previousOff = false, nowOff = true))
    }
}
