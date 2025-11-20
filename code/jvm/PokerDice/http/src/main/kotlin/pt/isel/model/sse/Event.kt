package pt.isel.model.sse

import java.time.Instant

sealed interface Event {
    data class Message(
        val id: Long,
        val userId: Int,
        val msg: String,
        val groupName: String,
    ) : Event

    data class KeepAlive(
        val timestamp: Instant,
    ) : Event
}