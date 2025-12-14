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
        val timestamp: String,
    ) : Event

    data class NewLobby(
        val lobbyId: Int,
        val lobbyName: String,
    ) : Event

    data class LobbyUpdated(
        val lobbyId: Int,
    ) : Event

    data class LobbyClosed(
        val lobbyId: Int,
    ) : Event

    data class GameStarted(
        val lobbyId: Int,
        val gameId: Int,
    ) : Event

    data class RoundUpdate(
        val gameId: Int,
        val roundNumber: Int,
    ) : Event

    data class RoundEnded(
        val gameId: Int,
        val roundNumber: Int,
        val winnerId: Int,
    ) : Event

    data class TurnChanged(
        val gameId: Int,
        val turnUserId: Int,
        val roundNumber: Int,
    ) : Event

    data class DiceRolled(
        val gameId: Int,
        val userId: Int,
        val dice: List<String>,
    ) : Event

    data class GameUpdated(
        val gameId: Int,
    ) : Event

    data class GameEnded(
        val gameId: Int,
    ) : Event

    data class KeepAlive(
        val timestamp: Instant,
    ) : Event

    data class CountdownStarted(
        val lobbyId: Int,
        val expiresAt: Long,
    ) : Event
}
