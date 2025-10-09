package pt.isel.model.lobby

import pt.isel.domain.Lobby
import pt.isel.domain.User

data class LobbyCreateInputModel(
    val name: String,
    val description: String = "",
    val minPlayers: Int,
)

data class LobbyPlayerOutputModel(
    val id: Int,
    val name: String,
    val email: String,
)

data class LobbyOutputModel(
    val id: Int,
    val name: String,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val players: List<LobbyPlayerOutputModel>,
    val hostId: Int,
)

data class LobbyListOutputModel(
    val lobbies: List<LobbyOutputModel>,
)

fun Lobby.toOutputModel(): LobbyOutputModel =
    LobbyOutputModel(
        id = id,
        name = name,
        description = description,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        players = users.map(User::toLobbyPlayer),
        hostId = host.id,
    )

fun User.toLobbyPlayer() = LobbyPlayerOutputModel(id, name, email)
