package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Lobby
import pt.isel.domain.users.User

@Component
interface RepositoryLobby : Repository<Lobby> {
    fun createLobby(
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        host: User,
    ): Lobby

    fun findByName(name: String): Lobby?

    fun deleteLobbyByHost(host: User)

    fun deleteLobbyById(id: Int)
}
