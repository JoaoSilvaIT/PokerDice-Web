package pt.isel.mem

import pt.isel.RepositoryLobby
import pt.isel.domain.Lobby
import pt.isel.domain.User
import pt.isel.utilis.MAX_PLAYERS

class RepositoryLobbyInMem : RepositoryLobby {
    private val lobbies = mutableListOf<Lobby>()
    private var lobby = 0

    override fun deleteLobbyByHost(host: User) {
        lobbies.removeAll{ it.host == host }
    }

    override fun createLobby(name: String, description: String, minPlayers: Int, host: User): Lobby {
        val lobby = Lobby(lobby++, name, description, minPlayers, MAX_PLAYERS, listOf(host), host)
        lobbies.add(lobby)
        return lobby
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

    override fun clear() = lobbies.clear()
}