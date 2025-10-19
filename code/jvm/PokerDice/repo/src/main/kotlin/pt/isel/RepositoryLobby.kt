package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.lobby.LobbyExternalInfo
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

    fun getLobbyById(id: Int): LobbyExternalInfo

    fun findByName(name: String): LobbyExternalInfo?

    fun deleteLobbyByHost(host: User)

    fun deleteLobbyById(id: Int)
}
