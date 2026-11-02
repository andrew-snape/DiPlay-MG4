package com.shilapi.xcertplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SunPositionTest {
    // Melbourne CBD.
    private val lat = -37.8136
    private val lon = 144.9631

    private fun at(utc: String) = Instant.parse(utc).toEpochMilli()

    @Test
    fun middayIsDayAndMidnightIsNight() {
        assertFalse(SunPosition.isNight(lat, lon, at("2026-11-02T02:00:00Z"))) // 13:00 AEDT
        assertTrue(SunPosition.isNight(lat, lon, at("2026-11-02T13:00:00Z"))) // 00:00 AEDT
    }

    @Test
    fun switchesWithinMinutesOfPublishedSunset() {
        // Melbourne sunset on 2026-11-02 is about 20:00 AEDT (09:00 UTC).
        assertFalse(SunPosition.isNight(lat, lon, at("2026-11-02T08:50:00Z")))
        assertTrue(SunPosition.isNight(lat, lon, at("2026-11-02T09:10:00Z")))
    }

    @Test
    fun switchesWithinMinutesOfPublishedSunrise() {
        // Melbourne sunrise on 2026-06-21 is about 07:35 AEST (21:35 UTC the day before).
        assertTrue(SunPosition.isNight(lat, lon, at("2026-06-20T21:25:00Z")))
        assertFalse(SunPosition.isNight(lat, lon, at("2026-06-20T21:45:00Z")))
    }

    @Test
    fun noonSunHeightMatchesLatitudeAndSeason() {
        // Equator at the March equinox: the sun is close to overhead at local noon.
        assertEquals(90.0, SunPosition.elevationDegrees(0.0, 0.0, at("2026-03-20T12:07:00Z")), 1.0)
    }
}
