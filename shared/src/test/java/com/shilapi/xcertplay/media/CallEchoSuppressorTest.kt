package com.shilapi.xcertplay.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallEchoSuppressorTest {
    private fun pcm(value: Int, samples: Int = 960): ByteArray =
        ByteArray(samples * 2).also { bytes ->
            for (i in 0 until samples) {
                bytes[2 * i] = value.toByte()
                bytes[2 * i + 1] = (value shr 8).toByte()
            }
        }

    private fun level(frame: ByteArray) = CallEchoSuppressor.rms(frame, 0, frame.size)

    @Test
    fun microphoneIsUntouchedBeforeAnyFarEndAudio() {
        val suppressor = CallEchoSuppressor()
        val frame = pcm(3000)
        assertFalse(suppressor.process(frame, 48_000, nowNs = 0))
        assertEquals(3000, level(frame))
    }

    @Test
    fun microphoneIsTurnedDownWhileTheFarEndIsLoud() {
        val suppressor = CallEchoSuppressor()
        suppressor.noteFarEnd(pcm(4000), 0, 1920, nowNs = 0)
        var frame = pcm(3000)
        repeat(5) { frame = pcm(3000); suppressor.process(frame, 48_000, nowNs = 10_000_000L) }
        assertTrue("level=${level(frame)}", level(frame) < 3000 * 0.2)
    }

    @Test
    fun quietFarEndDoesNotSuppress() {
        val suppressor = CallEchoSuppressor()
        suppressor.noteFarEnd(pcm(50), 0, 1920, nowNs = 0)
        val frame = pcm(3000)
        assertFalse(suppressor.process(frame, 48_000, nowNs = 1_000_000L))
        assertEquals(3000, level(frame))
    }

    @Test
    fun microphoneRecoversAfterTheHoldWindow() {
        val suppressor = CallEchoSuppressor(holdNanos = 100_000_000L)
        suppressor.noteFarEnd(pcm(4000), 0, 1920, nowNs = 0)
        repeat(3) { suppressor.process(pcm(3000), 48_000, nowNs = 10_000_000L) }
        var frame = pcm(3000)
        repeat(20) { frame = pcm(3000); suppressor.process(frame, 48_000, nowNs = 500_000_000L) }
        assertEquals(3000, level(frame))
    }

    @Test
    fun negativeSamplesKeepTheirSignWhenScaled() {
        val suppressor = CallEchoSuppressor()
        suppressor.noteFarEnd(pcm(4000), 0, 1920, nowNs = 0)
        var frame = pcm(-3000)
        repeat(5) { frame = pcm(-3000); suppressor.process(frame, 48_000, nowNs = 10_000_000L) }
        val last = (frame[frame.size - 2].toInt() and 0xFF) or (frame[frame.size - 1].toInt() shl 8)
        assertTrue("last=$last", last < 0 && last > -1000)
    }
}
