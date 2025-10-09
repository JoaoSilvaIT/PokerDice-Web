package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.Lobby
import pt.isel.domain.User
import pt.isel.errors.LobbyError
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

@Component
class LobbyService(
    private val repoLobby: RepositoryLobby,
) {
    fun createLobby(
        host: User,
        name: String,
        description: String,
        minPlayers: Int,
    ): Either<LobbyError, Lobby> {
        if (name.isBlank()) return failure(LobbyError.BlankName)
        if (minPlayers < 2) return failure(LobbyError.MinPlayersTooLow)

        val trimmedName = name.trim()
        val trimmedDesc = description.trim()
        if (repoLobby.findByName(trimmedName) != null) return failure(LobbyError.NameAlreadyUsed)
        return success(repoLobby.createLobby(trimmedName, trimmedDesc, minPlayers, host))
    }

    fun listVisibleLobbies(): List<Lobby> = repoLobby.findAll().filter { it.users.size < it.maxPlayers }

    fun joinLobby(
        lobbyId: Int,
        user: User,
    ): Either<LobbyError, Lobby> {
        val lobby = repoLobby.findById(lobbyId) ?: return Either.Failure(LobbyError.LobbyNotFound)
        if (lobby.users.size >= lobby.maxPlayers) return Either.Failure(LobbyError.LobbyFull)
        if (lobby.users.any { it.id == user.id }) return success(lobby) // already joined
        val updated = lobby.copy(users = lobby.users + user)
        repoLobby.save(updated)
        return success(updated)
    }

    fun leaveLobby(
        lobbyId: Int,
        user: User,
    ): Either<LobbyError, Boolean> {
        val lobby = repoLobby.findById(lobbyId) ?: return Either.Failure(LobbyError.LobbyNotFound)
        if (lobby.host.id == user.id) {
            // Host leaves before match starts: close lobby
            repoLobby.deleteLobbyByHost(user)
            return success(true)
        }
        val updated = lobby.copy(users = lobby.users.filter { it.id != user.id })
        repoLobby.save(updated)
        return success(false)
    }

    fun closeLobby(
        lobbyId: Int,
        host: User,
    ): Either<LobbyError, Unit> {
        val lobby = repoLobby.findById(lobbyId) ?: return Either.Failure(LobbyError.LobbyNotFound)
        if (lobby.host.id != host.id) return Either.Failure(LobbyError.NotHost)
        repoLobby.deleteLobbyById(lobbyId)
        return success(Unit)
    }
}
