package com.shilapi.xcertplay.media

import com.shilapi.xcertplay.iap2.message.Iap2Messages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarPlayCallTrackerTest {
    private fun update(
        uuid: String,
        status: Int? = null,
        direction: Int? = null,
        remote: String? = null,
        name: String? = null,
    ) = Iap2Messages.buildRaw(CarPlayCallTracker.CALL_STATE_UPDATE) {
        remote?.let { string(0, it) }
        name?.let { string(1, it) }
        status?.let { u8(2, it) }
        direction?.let { u8(3, it) }
        string(4, uuid)
    }

    @Test
    fun ringingIsIncomingThenAnsweredThenEnded() {
        val tracker = CarPlayCallTracker()
        val ring = tracker.accept(update("A", 2, 1, "0400111222", "Sam"))
        assertEquals(CarPlayCallEvent.INCOMING, ring?.event)
        assertEquals("Sam", ring?.call?.displayName)

        // Only the status changed: name and number are kept.
        val answered = tracker.accept(update("A", 4))
        assertEquals(CarPlayCallEvent.UPDATED, answered?.event)
        assertEquals("0400111222", answered?.call?.remoteId)

        assertNull(tracker.accept(update("A", 4)))
        assertEquals(CarPlayCallEvent.ENDED, tracker.accept(update("A", 0))?.event)
        assertEquals(0, tracker.activeCount)
    }

    @Test
    fun sendingIsOutgoing() {
        val tracker = CarPlayCallTracker()
        assertEquals(CarPlayCallEvent.OUTGOING, tracker.accept(update("B", 1, 2, "123", "Home"))?.event)
    }

    @Test
    fun firstSeenActiveFollowsDirection() {
        val tracker = CarPlayCallTracker()
        assertEquals(CarPlayCallEvent.OUTGOING, tracker.accept(update("C", 4, 2, "1", "x"))?.event)
        assertEquals(CarPlayCallEvent.INCOMING, tracker.accept(update("D", 3, 1, "2", "y"))?.event)
    }

    @Test
    fun unknownDirectionWaitsAndEndedUnknownIsIgnored() {
        val tracker = CarPlayCallTracker()
        assertNull(tracker.accept(update("E", 4, 0)))
        assertEquals(0, tracker.activeCount)
        assertNull(tracker.accept(update("F", 0)))
        assertNull(tracker.accept(update("F", 6, 1)))
    }

    @Test
    fun disconnectingEndsAndClearEndsEverything() {
        val tracker = CarPlayCallTracker()
        tracker.accept(update("G", 2, 1))
        assertEquals(CarPlayCallEvent.ENDED, tracker.accept(update("G", 6))?.event)
        tracker.accept(update("H", 2, 1))
        tracker.accept(update("I", 1, 2))
        val ended = tracker.clear()
        assertEquals(setOf("H", "I"), ended.map { it.call.uuid }.toSet())
        assertEquals(0, tracker.activeCount)
    }

    @Test
    fun otherMessagesAndFramesWithoutUuidAreIgnored() {
        val tracker = CarPlayCallTracker()
        assertNull(tracker.accept(Iap2Messages.buildRaw(0x5001) { u8(2, 2) }))
        assertNull(tracker.accept(Iap2Messages.buildRaw(CarPlayCallTracker.CALL_STATE_UPDATE) { u8(2, 2) }))
    }
}
