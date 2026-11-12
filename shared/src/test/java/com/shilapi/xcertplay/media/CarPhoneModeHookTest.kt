package com.shilapi.xcertplay.media

import org.junit.Assert.assertEquals
import org.junit.Test

class CarPhoneModeHookTest {
    @Test
    fun turnsOnOnceAndOffOnce() {
        val calls = mutableListOf<Boolean>()
        val hook = CarPhoneModeHook({ calls += it; 0 })
        hook.set(true, "call")
        hook.set(true, "call again")
        hook.release("stop")
        hook.release("session stop")
        assertEquals(listOf(true, false), calls)
    }

    @Test
    fun aFailedOnIsNeverFollowedByOff() {
        val calls = mutableListOf<Boolean>()
        val hook = CarPhoneModeHook({ calls += it; null })
        hook.set(true, "call")
        hook.release("stop")
        assertEquals(listOf(true), calls)
    }

    @Test
    fun usesTheStockDeviceAndName() {
        assertEquals(0x400, Mg4CarPhoneMode.DEVICE)
        assertEquals("carplay_source=phone", Mg4CarPhoneMode.NAME)
    }
}
