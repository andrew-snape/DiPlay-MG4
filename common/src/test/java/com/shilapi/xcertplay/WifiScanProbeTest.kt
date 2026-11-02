package com.shilapi.xcertplay

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiScanProbeTest {
    @Test
    fun linesShowTheGapBetweenScans() {
        assertEquals("Wi-Fi scan done updated=true results=12 sinceLast=first", WifiScanProbe.line(true, null, 12))
        assertEquals("Wi-Fi scan done updated=false results=? sinceLast=160s", WifiScanProbe.line(false, 160_400, null))
    }
}
