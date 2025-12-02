package pt.isel.domain.sse

import java.time.Instant

sealed interface Event {
    data class PlayerJoined(
        val lobbyId: Int,
        val userId: Int,
        val playerName: String,
    ) : Event

    data class PlayerLeft(
        val lobbyId: Int,
        val playerId: Int,
        val timestamp: String
    ) : Event

    data class KeepAlive(
        val timestamp: Instant,
    ) : Event
}