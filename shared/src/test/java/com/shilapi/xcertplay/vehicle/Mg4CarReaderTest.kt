package com.shilapi.xcertplay.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Mg4CarReaderTest {
    private class FakeSignals(
        var soc: Float? = null,
        var range: Float? = null,
        var status: Int? = null,
        var port: Boolean? = null,
        var power: Float? = null,
        var energy: Float? = null,
        var capacity: Float? = null,
    ) : Mg4Signals {
        override fun socPercent() = soc
        override fun rangeKm() = range
        override fun chargingStatus() = status
        override fun chargePortConnected() = port
        override fun batteryPowerKw() = power
        override fun batteryEnergyKwh() = energy
        override fun batteryCapacityKwh() = capacity
        override fun rawGear(): Int? = null
        override fun speedMetersPerSecond(): Float? = null
    }

    @Test
    fun passesPlausibleValuesThrough() {
        val reading = Mg4CarReader(FakeSignals(soc = 64f, range = 312.4f, status = 1, port = true,
            power = -7f, energy = 40f, capacity = 62f)).read()
        assertEquals(64f, reading.socPercent!!, 1e-6f)
        assertEquals(312, reading.rangeKm)
        assertEquals(1, reading.chargingStatus)
        assertEquals(true, reading.chargePortConnected)
        assertEquals(-7f, reading.batteryPowerKw!!, 1e-6f)
        assertEquals(40f, reading.batteryEnergyKwh!!, 1e-6f)
        assertEquals(62f, reading.batteryCapacityKwh!!, 1e-6f)
    }

    @Test
    fun implausibleOrMissingValuesBecomeNull() {
        val reading = Mg4CarReader(FakeSignals(soc = 120f, range = -1f, status = -1,
            power = Float.NaN, energy = -3f, capacity = 0f)).read()
        assertNull(reading.socPercent)
        assertNull(reading.rangeKm)
        assertNull(reading.chargingStatus)
        assertNull(reading.batteryPowerKw)
        assertNull(reading.batteryEnergyKwh)
        assertNull(reading.batteryCapacityKwh)
        assertNull(Mg4CarReader(FakeSignals()).read().socPercent)
    }

    @Test
    fun firmwareGateIsSwi165Only() {
        assertTrue(Mg4Firmware.isSwi165("SWI165-29176-1100R11"))
        assertTrue(Mg4Firmware.isSwi165("  SWI165-29176-1100R11 "))
        assertFalse(Mg4Firmware.isSwi165("SWI69-12345-1000R01"))
        assertFalse(Mg4Firmware.isSwi165("SWI68-29958-1300R67"))
        assertFalse(Mg4Firmware.isSwi165(""))
        assertFalse(Mg4Firmware.isSwi165(null))
    }
}
