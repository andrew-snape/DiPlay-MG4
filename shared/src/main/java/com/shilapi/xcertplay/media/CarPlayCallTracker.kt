package com.shilapi.xcertplay.media

import com.shilapi.xcertplay.iap2.body.Iap2BodyReader
import com.shilapi.xcertplay.iap2.wire.Iap2Frame

/** One CarPlay call as the iPhone reports it in iAP2 CallStateUpdate (0x4155). */
data class CarPlayCall(
    val uuid: String,
    val remoteId: String,
    val displayName: String,
    /** iAP2 call status: 0 disconnected, 1 sending, 2 ringing, 3 connecting, 4 active, 5 held, 6 disconnecting. */
    val status: Int,
    /** iAP2 call direction: 0 unknown, 1 incoming, 2 outgoing. */
    val direction: Int,
) {
    val ended: Boolean get() = status == STATUS_DISCONNECTED || status == STATUS_DISCONNECTING

    companion object {
        const val STATUS_DISCONNECTED = 0
        const val STATUS_SENDING = 1
        const val STATUS_RINGING = 2
        const val STATUS_CONNECTING = 3
        const val STATUS_ACTIVE = 4
        const val STATUS_HELD = 5
        const val STATUS_DISCONNECTING = 6
        const val DIRECTION_INCOMING = 1
        const val DIRECTION_OUTGOING = 2
    }
}

/** What Telecom should be told about a CarPlay call. */
enum class CarPlayCallEvent {
    /** A new call is ringing: Telecom `addNewIncomingCall`. */
    INCOMING,

    /** A new call that is outgoing or already running: Telecom `addNewUnknownCall`. */
    OUTGOING,

    /** A known call changed state (answered, held, connecting). */
    UPDATED,

    /** A known call ended. */
    ENDED,
}

data class CarPlayCallChange(val event: CarPlayCallEvent, val call: CarPlayCall)

/**
 * Turns CallStateUpdate frames into call events, as the stock app's MirroringControllerService
 * does: ringing (2) is an incoming call, sending (1) an outgoing one, and a call first seen in
 * another state follows its direction. Disconnected (0) or disconnecting (6) ends a known call
 * and is ignored for an unknown one. Frames carry only what changed, so a known call keeps its
 * earlier name and number. Pure logic: no Android classes.
 */
class CarPlayCallTracker {
    private val calls = LinkedHashMap<String, CarPlayCall>()

    val activeCount: Int get() = synchronized(calls) { calls.size }

    /** The change [frame] causes, or null when it is not a call update or changes nothing. */
    fun accept(frame: Iap2Frame): CarPlayCallChange? {
        if (frame.messageId != CALL_STATE_UPDATE) return null
        val parsed = runCatching {
            val body = Iap2BodyReader.of(frame)
            val uuid = body.optionalString(UUID)
            if (uuid == null) null else Update(
                uuid = uuid,
                remoteId = body.optionalString(REMOTE_ID),
                displayName = body.optionalString(DISPLAY_NAME),
                status = body.optionalU8(STATUS),
                direction = body.optionalU8(DIRECTION),
            )
        }.getOrNull() ?: return null
        return accept(parsed)
    }

    internal data class Update(
        val uuid: String,
        val remoteId: String?,
        val displayName: String?,
        val status: Int?,
        val direction: Int?,
    )

    internal fun accept(update: Update): CarPlayCallChange? = synchronized(calls) {
        val known = calls[update.uuid]
        val call = CarPlayCall(
            uuid = update.uuid,
            remoteId = update.remoteId ?: known?.remoteId.orEmpty(),
            displayName = update.displayName ?: known?.displayName.orEmpty(),
            status = update.status ?: known?.status ?: CarPlayCall.STATUS_DISCONNECTED,
            direction = update.direction ?: known?.direction ?: 0,
        )
        if (known == null) {
            if (call.ended) return null
            val event = when {
                call.status == CarPlayCall.STATUS_RINGING -> CarPlayCallEvent.INCOMING
                call.status == CarPlayCall.STATUS_SENDING -> CarPlayCallEvent.OUTGOING
                call.direction == CarPlayCall.DIRECTION_INCOMING -> CarPlayCallEvent.INCOMING
                call.direction == CarPlayCall.DIRECTION_OUTGOING -> CarPlayCallEvent.OUTGOING
                // Direction not known yet: wait for it rather than guess.
                else -> return null
            }
            calls[call.uuid] = call
            return CarPlayCallChange(event, call)
        }
        if (call.ended) {
            calls.remove(call.uuid)
            return CarPlayCallChange(CarPlayCallEvent.ENDED, call)
        }
        if (call == known) return null
        calls[call.uuid] = call
        CarPlayCallChange(CarPlayCallEvent.UPDATED, call)
    }

    /** The session ended: every known call ends. */
    fun clear(): List<CarPlayCallChange> = synchronized(calls) {
        calls.values.map { CarPlayCallChange(CarPlayCallEvent.ENDED, it.copy(status = CarPlayCall.STATUS_DISCONNECTED)) }
            .also { calls.clear() }
    }

    companion object {
        const val CALL_STATE_UPDATE = 0x4155
        private const val REMOTE_ID = 0
        private const val DISPLAY_NAME = 1
        private const val STATUS = 2
        private const val DIRECTION = 3
        private const val UUID = 4
    }
}
