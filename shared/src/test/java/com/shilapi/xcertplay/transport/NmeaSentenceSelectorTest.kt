package com.shilapi.xcertplay.transport

import com.shilapi.xcertplay.iap2.body.Iap2BodyReader
import com.shilapi.xcertplay.iap2.message.Iap2Messages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NmeaSentenceSelectorTest {
    private val gga = "\$GPGGA,123519.00,4807.0380,N,01131.0000,E,1,08,1.0,545.4,M,0.0,M,,*00"
    private val rmc = "\$GPRMC,123519.00,A,4807.0380,N,01131.0000,E,0.0,0.0,020126,,,A*00"
    private val pascd = "\$PASCD,17877.092,C,D,0,1,0.00,10.000*00"
    private val all = "$gga\r\n$rmc\r\n$pascd\r\n"
    private var nowMillis = 0L
    private val selector = NmeaSentenceSelector { nowMillis }

    @Test
    fun sendsOnlyTheRequestedSentenceTypes() {
        selector.reset(setOf(0, 1, 4), emptyMap())
        assertEquals("$gga\r\n$pascd\r\n", selector.select(all))
    }

    @Test
    fun aRequestWithoutKnownTypesStillGetsEverything() {
        selector.reset(setOf(0), emptyMap())
        assertEquals(all, selector.select(all))
    }

    @Test
    fun honoursIntervalsLongerThanASecond() {
        selector.reset(setOf(1, 2), mapOf(1 to 1_000L, 2 to 5_000L))
        assertEquals("$gga\r\n$rmc\r\n", selector.select(all))
        nowMillis = 1_000
        assertEquals("$gga\r\n", selector.select(all))
        nowMillis = 5_000
        assertEquals("$gga\r\n$rmc\r\n", selector.select(all))
    }

    @Test
    fun nothingSelectedSendsNothing() {
        selector.reset(setOf(2), emptyMap())
        assertNull(selector.select("$gga\r\n"))
    }

    @Test
    fun readsIntervalsFromTheRequest() {
        val start = Iap2Messages.buildRaw(Iap2LocationMessages.START_LOCATION_INFORMATION) {
            u16(0, 0)
            void(1)
            void(2)
            void(4)
            u32(0x8001, 1000)
            u32(0x8004, 250)
        }
        assertEquals(mapOf(1 to 1_000L, 4 to 250L), Iap2LocationMessages.requestedIntervals(start))
    }

    @Test
    fun eachSentenceGoesInItsOwnParameter() {
        val frame = Iap2LocationMessages.locationInformation(all)
        val parameters = Iap2BodyReader.of(frame).list()
        assertEquals(listOf(0, 0, 0), parameters.map { it.id })
        assertEquals("$rmc\r\n\u0000", parameters[1].payload.decodeToString())
    }
}
