package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Lobby
import pt.isel.domain.users.User
import pt.isel.errors.LobbyError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

@Component
class LobbyService(
    private val trxManager: TransactionManager,
) {
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
            success(repoLobby.createLobby(trimmedName, trimmedDesc, minPlayers, maxPlayers, host))
        }
    }

    fun listVisibleLobbies(): List<Lobby> =
        trxManager.run {
            repoLobby.findAll().filter { it.users.size < it.maxPlayers }
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
            if (lobby.users.size >= lobby.maxPlayers) return@run failure(LobbyError.LobbyFull)
            if (lobby.users.any { it.id == user.id }) return@run failure(LobbyError.UserAlreadyInLobby)
            val updated = lobby.copy(users = lobby.users + user)
            repoLobby.save(updated)
            success(updated)
        }

    // Overloaded method for bulk adding users - used for testing
    fun joinLobby(
        lobbyId: Int,
        users: List<User>,
    ): Either<LobbyError, Lobby> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyError.LobbyNotFound)
            if (lobby.users.size + users.size > lobby.maxPlayers) return@run failure(LobbyError.LobbyFull)

            val updated = lobby.copy(users = lobby.users + users)
            repoLobby.save(updated)
            success(updated)
        }

    fun leaveLobby(
        lobbyId: Int,
        user: User,
    ): Either<LobbyError, Boolean> =
        trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(LobbyError.LobbyNotFound)
            if (lobby.host.id == user.id) {
                repoLobby.deleteLobbyByHost(user)
                return@run success(true)
            }
            val updated = lobby.copy(users = lobby.users.filter { it.id != user.id })
            repoLobby.save(updated)
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
            success(Unit)
        }
}
