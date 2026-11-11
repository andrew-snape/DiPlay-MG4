package com.shilapi.xcertplay.media

import java.util.concurrent.atomic.AtomicLong

/**
 * Voice-activated echo suppression for calls.
 *
 * The SWI165 head unit has no Android echo canceller (`aec=unavailable`, car test 2026-11-11), so
 * the other person's voice, played through the car speakers, goes back into the microphone and
 * returns to them. The call audio the app plays is the reference: while it is loud, the
 * microphone is turned down by [floorGain], then eased back up once the far end has been quiet
 * for [holdNanos] (the speaker and cabin delay).
 *
 * This is suppression, not cancellation: someone talking over the caller is also quietened.
 */
internal class CallEchoSuppressor(
    private val holdNanos: Long = DEFAULT_HOLD_NANOS,
    private val floorGain: Float = DEFAULT_FLOOR_GAIN,
    private val activeRms: Int = ACTIVE_RMS,
) {
    private val lastFarActiveNs = AtomicLong(NEVER)
    @Volatile private var gain = 1f
    @Volatile var suppressedFrames = 0
        private set

    /** Call with each block of PCM16 little-endian the call track plays. */
    fun noteFarEnd(pcm: ByteArray, offset: Int, length: Int, nowNs: Long = System.nanoTime()) {
        if (rms(pcm, offset, length) >= activeRms) lastFarActiveNs.set(nowNs)
    }

    /** Scales one captured PCM16 little-endian frame in place. Returns true if it was turned down. */
    fun process(frame: ByteArray, sampleRate: Int, nowNs: Long = System.nanoTime()): Boolean {
        val last = lastFarActiveNs.get()
        val farActive = last != NEVER && nowNs - last <= holdNanos
        val target = if (farActive) floorGain else 1f
        val samples = frame.size / 2
        if (samples == 0) return false
        // Down fast (about 5 ms), up slowly (about 60 ms), so words are not clipped on release.
        val rate = sampleRate.coerceAtLeast(8_000)
        val step = if (target < gain) (1f - floorGain) / (rate * 0.005f) else (1f - floorGain) / (rate * 0.06f)
        var current = gain
        var changed = false
        for (i in 0 until samples) {
            if (current < target) current = minOf(target, current + step)
            else if (current > target) current = maxOf(target, current - step)
            if (current < 0.999f) {
                val value = (frame[2 * i].toInt() and 0xFF) or (frame[2 * i + 1].toInt() shl 8)
                val scaled = (value * current).toInt().coerceIn(-32768, 32767)
                frame[2 * i] = scaled.toByte()
                frame[2 * i + 1] = (scaled shr 8).toByte()
                changed = true
            }
        }
        gain = current
        if (changed) suppressedFrames++
        return changed
    }

    fun resetCounters() {
        suppressedFrames = 0
    }

    companion object {
        private const val NEVER = Long.MIN_VALUE
        /** Speaker buffer (about 190 ms) plus the cabin path, with some margin. */
        const val DEFAULT_HOLD_NANOS = 600_000_000L
        /** About -16 dB. */
        const val DEFAULT_FLOOR_GAIN = 0.15f
        /** PCM16 rms of about -44 dBFS: quieter than that is line noise, not speech. */
        const val ACTIVE_RMS = 200

        /** Shared by the call playback (reference) and the call microphone (uplink). */
        val shared = CallEchoSuppressor()

        fun rms(pcm: ByteArray, offset: Int, length: Int): Int {
            val samples = length / 2
            if (samples <= 0) return 0
            var sum = 0.0
            for (i in 0 until samples) {
                val p = offset + 2 * i
                val value = (pcm[p].toInt() and 0xFF) or (pcm[p + 1].toInt() shl 8)
                sum += value.toDouble() * value
            }
            return Math.sqrt(sum / samples).toInt()
        }
    }
}
