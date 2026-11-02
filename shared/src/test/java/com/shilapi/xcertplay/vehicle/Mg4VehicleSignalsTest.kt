package com.shilapi.xcertplay.vehicle

import com.shilapi.xcertplay.transport.VehicleGear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Mg4VehicleSignalsTest {
    @Test
    fun readsSpeedInMetersPerSecond() {
        assertEquals(0.0, Mg4WheelSpeed.metersPerSecond(0f)!!, 1e-9)
        assertEquals(10.0, Mg4WheelSpeed.metersPerSecond(36f)!!, 1e-6)
    }

    @Test
    fun rejectsMissingOrImpossibleSpeed() {
        assertNull(Mg4WheelSpeed.metersPerSecond(null))
        assertNull(Mg4WheelSpeed.metersPerSecond(-1f))
        assertNull(Mg4WheelSpeed.metersPerSecond(Float.NaN))
        assertNull(Mg4WheelSpeed.metersPerSecond(301f))
    }

    @Test
    fun onlyParkIsConfirmedSoOtherGearsDrive() {
        assertEquals(VehicleGear.PARK, Mg4WheelSpeed.gear(1))
        assertEquals(VehicleGear.DRIVE, Mg4WheelSpeed.gear(2))
        assertEquals(VehicleGear.DRIVE, Mg4WheelSpeed.gear(4))
        assertNull(Mg4WheelSpeed.gear(null))
        assertNull(Mg4WheelSpeed.gear(0))
        assertNull(Mg4WheelSpeed.gear(-1))
    }

    @Test
    fun carChargingStatusDecidesWhenPresent() {
        assertTrue(Mg4Charging.isCharging(1, true, null)) // AC
        assertTrue(Mg4Charging.isCharging(10, true, null)) // DC
        // Plugged in, but connecting, waiting or stopped: Apple Maps should not show charging.
        assertFalse(Mg4Charging.isCharging(5, true, -7f))
        assertFalse(Mg4Charging.isCharging(7, true, null))
        assertFalse(Mg4Charging.isCharging(8, true, null))
        assertFalse(Mg4Charging.isCharging(0, false, null))
    }

    @Test
    fun fallsBackToPlugOrPowerWithoutStatus() {
        assertTrue(Mg4Charging.isCharging(null, true, null))
        assertTrue(Mg4Charging.isCharging(null, null, -7f))
        assertFalse(Mg4Charging.isCharging(null, false, 12f))
        assertFalse(Mg4Charging.isCharging(null, null, null))
    }
}
