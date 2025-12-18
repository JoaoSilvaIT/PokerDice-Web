package pt.isel

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo
import pt.isel.errors.LobbyError
import pt.isel.repo.TransactionManager
import pt.isel.timeout.LobbyTimeoutManager
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success
import java.time.Instant

@Component
class LobbyService(
    private val trxManager: TransactionManager,
    private val lobbyEvents: LobbyEventService,
    private val lobbyTimeouts: LobbyTimeoutManager,
    private val gameService: GameService,
    @Value("\${pokerdice.lobby.timeout-seconds:10}") private val lobbyTimeout: Long,
) {
    init {
        lobbyTimeouts.registerStartHandler { lobbyId ->
            trxManager.run {
                val lobby = repoLobby.findById(lobbyId) ?: return@run
                val game =
                    gameService.createGame(
                        System.currentTimeMillis(),
                        lobby.id,
                        lobby.players.size,
                        lobby.host.id,
                    )

                when (game) {
                    is Either.Failure -> {
                        return@run
                    }
                    is Either.Success -> {
                        val game = game.value
                        lobbyEvents.notifyGameCreated(
                            lobbyId = lobby.id,
                            gameId = game.id,
                        )
                    }
                }
            }
        }
    }

    fun createLobby(
        host: User,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
    ): Either<LobbyError, Lobby> {
        if (name.isBlank()) return failure(LobbyError.BlankName)
        if (minPlayers < 2) return failure(LobbyError.MinPlayersTooLow)
        if (maxPlayers > 10) return failure(LobbyError.MaxPlayersTooHigh)
        if (minPlayers > maxPlayers) return failure(LobbyError.MinPlayersTooLow)

        val trimmedName = name.trim()
        val trimmedDesc = description.trim().take(500)

        return trxManager.run {
            if (repoLobby.findByName(trimmedName) != null) {
                return@run failure(LobbyError.NameAlreadyUsed)
            }
            val lobby = repoLobby.createLobby(trimmedName, trimmedDesc, minPlayers, maxPlayers, host, lobbyTimeout)

            lobbyEvents.notifyNewLobby(lobby.id, lobby.name)
            success(lobby)
        }
    }

    fun listVisibleLobbies(): List<Lobby> =
        trxManager.run {
            repoLobby.findAll().filter { it.players.size < it.settings.maxPlayers }
        }

    fun findLobbyById(lobbyId: Int): Lobby? =
        trxManager.run {
            repoLobby.findById(lobbyId)
        }

    fun joinLobby(
        lobbyId: Int,
        user: User,
    ): Either<LobbyError, Lobby> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyError.LobbyNotFound)

            val activeGames = repoGame.findActiveGamesByLobbyId(lobbyId)
            if (activeGames.isNotEmpty()) {
                return@run failure(LobbyError.GameAlreadyStarted)
            }

            if (lobby.players.size >= lobby.settings.maxPlayers) return@run failure(LobbyError.LobbyFull)
            if (lobby.players.any { it.id == user.id }) return@run failure(LobbyError.UserAlreadyInLobby)
            val updated = lobby.copy(players = lobby.players + UserExternalInfo(user.id, user.name, user.balance))
            repoLobby.save(updated)

            lobbyEvents.notifyPlayerJoined(lobbyId, user.id, user.name)
            lobbyEvents.notifyLobbyUpdated(lobbyId)
            if (updated.players.size >= updated.settings.minPlayers) {
                lobbyTimeouts.startCountdown(
                    lobbyId = lobbyId,
                    seconds = lobbyTimeout,
                )
                lobbyEvents.notifyCountdownStarted(
                    lobbyId,
                    Instant.now().plusSeconds(lobbyTimeout).toEpochMilli(),
                )
            }
            success(updated)
        }

    fun leaveLobby(
        lobbyId: Int,
        user: User,
    ): Either<LobbyError, Boolean> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyError.LobbyNotFound)

            if (lobby.players.none { it.id == user.id }) {
                return@run success(false)
            }

            if (lobby.players.size == 1) {
                repoLobby.deleteLobbyById(lobbyId)

                lobbyEvents.notifyPlayerLeft(lobbyId, user.id, Instant.now().toString())
                lobbyEvents.notifyLobbyClosed(lobbyId)
                return@run success(true)
            }

            val updated = lobby.copy(players = lobby.players.filter { it.id != user.id }.toSet())
            repoLobby.save(updated)

            if (updated.players.size < updated.settings.minPlayers) {
                lobbyTimeouts.cancelCountdown(lobbyId)
                if (lobby.host.id == user.id) {
                    val newLobby = lobby.copy(host = updated.players.first(), players = updated.players)
                    repoLobby.save(newLobby)
                }
            }
            lobbyEvents.notifyPlayerLeft(lobbyId, user.id, Instant.now().toString())
            lobbyEvents.notifyLobbyUpdated(lobbyId)
            success(false)
        }

    fun closeLobby(
        lobbyId: Int,
        host: User,
    ): Either<LobbyError, Unit> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyError.LobbyNotFound)
            if (lobby.host.id != host.id) return@run failure(LobbyError.NotHost)
            repoLobby.deleteLobbyById(lobbyId)

            lobbyTimeouts.cancelCountdown(lobbyId)
            lobbyEvents.notifyLobbyClosed(lobbyId)
            success(Unit)
        }

    fun getLobby(lobbyId: Int): Either<LobbyError, Lobby> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyError.LobbyNotFound)
            success(lobby)
        }
}
