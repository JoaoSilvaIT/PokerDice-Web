package pt.isel

import pt.isel.domain.Lobby
import pt.isel.domain.User

interface RepositoryLobby : Repository<Lobby> {
    fun createLobby(
        name: String,
        description: String,
        minPlayers: Int,
        host : User
    ) : Lobby

    fun findByName(name: String): Lobby?

    fun deleteLobbyByHost(host: User)
}