package com.shilapi.xcertplay

import android.bluetooth.BluetoothClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhonePairingRulesTest {
    @Test fun phoneClassOrIphoneNameIsOffered() {
        assertTrue(PhonePairingRules.isLikelyPhone(BluetoothClass.Device.Major.PHONE, null))
        assertTrue(PhonePairingRules.isLikelyPhone(null, "Snape's iPhone"))
        assertFalse(PhonePairingRules.isLikelyPhone(BluetoothClass.Device.Major.AUDIO_VIDEO, "Headphones"))
        assertFalse(PhonePairingRules.isLikelyPhone(null, null))
    }

    @Test fun labelAddsAddressOnlyForSharedNames() {
        assertEquals("iPhone", PhonePairingRules.label("iPhone", "AA:BB:CC:DD:EE:FF", false, "Device"))
        assertEquals("iPhone · EE:FF", PhonePairingRules.label("iPhone", "AA:BB:CC:DD:EE:FF", true, "Device"))
        assertEquals("Device", PhonePairingRules.label(null, "AA:BB:CC:DD:EE:FF", false, "Device"))
    }
}
