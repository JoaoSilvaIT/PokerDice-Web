package pt.isel.mem

import pt.isel.RepositoryLobby
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.lobby.LobbyExternalInfo
import pt.isel.domain.lobby.LobbySettings
import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo

class RepositoryLobbyInMem : RepositoryLobby {
    private val lobbies = mutableListOf<Lobby>()
    private var lobbyIdCounter = 0

    override fun deleteLobbyByHost(host: User) {
        lobbies.removeAll { it.host.id == host.id }
    }

    override fun createLobby(
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        host: User,
    ): Lobby {
        val settings =
            LobbySettings(
                numberOfRounds = 3,
                minPlayers = minPlayers,
                maxPlayers = maxPlayers,
            )
        val hostInfo = UserExternalInfo(host.id, host.name, host.balance)
        val lobby =
            Lobby(
                id = lobbyIdCounter++,
                name = name,
                description = description,
                host = hostInfo,
                settings = settings,
                players = setOf(hostInfo),
            )
        lobbies.add(lobby)
        return lobby
    }

    override fun getLobbyById(id: Int): LobbyExternalInfo {
        val lobby = lobbies.firstOrNull { it.id == id }
        return if (lobby != null) {
            LobbyExternalInfo(
                name = lobby.name,
                description = lobby.description,
                currentPlayers = lobby.players.size,
                numberOfRounds = lobby.settings.numberOfRounds,
            )
        } else {
            throw NoSuchElementException("Lobby with id $id not found")
        }
    }

    override fun findByName(name: String) = lobbies.firstOrNull { it.name == name }

    override fun findById(id: Int) = lobbies.firstOrNull { it.id == id }

    override fun findAll() = lobbies

    override fun save(entity: Lobby) {
        lobbies.removeIf { it.id == entity.id }
        lobbies.add(entity)
    }

    override fun deleteById(id: Int) {
        lobbies.removeIf { it.id == id }
    }

    override fun deleteLobbyById(id: Int) {
        deleteById(id)
    }

    override fun clear() = lobbies.clear()
}
