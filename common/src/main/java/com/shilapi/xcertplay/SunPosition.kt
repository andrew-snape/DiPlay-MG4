package com.shilapi.xcertplay

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The sun's height above the horizon, for switching CarPlay to dark mode at sunset. Uses the
 * low-precision solar position from the Astronomical Almanac, good to about 0.1 degree, which is
 * well inside a minute of sunrise or sunset.
 */
internal object SunPosition {
    /** Official sunrise and sunset: the sun's upper edge on the horizon, allowing for refraction. */
    const val SUNSET_ELEVATION_DEGREES = -0.833

    private const val J2000_EPOCH_MILLIS = 946_728_000_000L // 2000-01-01 12:00 UTC
    private const val MILLIS_PER_DAY = 86_400_000.0

    fun elevationDegrees(latitudeDegrees: Double, longitudeDegrees: Double, epochMillis: Long): Double {
        val d = (epochMillis - J2000_EPOCH_MILLIS) / MILLIS_PER_DAY
        val meanAnomaly = Math.toRadians(357.529 + 0.98560028 * d)
        val meanLongitude = 280.459 + 0.98564736 * d
        val eclipticLongitude = Math.toRadians(
            meanLongitude + 1.915 * sin(meanAnomaly) + 0.020 * sin(2 * meanAnomaly),
        )
        val obliquity = Math.toRadians(23.439 - 0.00000036 * d)
        val rightAscension = atan2(cos(obliquity) * sin(eclipticLongitude), cos(eclipticLongitude))
        val declination = asin(sin(obliquity) * sin(eclipticLongitude))
        val siderealDegrees = (18.697374558 + 24.06570982441908 * d) * 15.0 + longitudeDegrees
        val hourAngle = Math.toRadians(siderealDegrees) - rightAscension
        val latitude = Math.toRadians(latitudeDegrees)
        return Math.toDegrees(
            asin(sin(latitude) * sin(declination) + cos(latitude) * cos(declination) * cos(hourAngle)),
        )
    }

    /** Between sunset and sunrise at this place. */
    fun isNight(latitudeDegrees: Double, longitudeDegrees: Double, epochMillis: Long): Boolean =
        elevationDegrees(latitudeDegrees, longitudeDegrees, epochMillis) < SUNSET_ELEVATION_DEGREES
}
