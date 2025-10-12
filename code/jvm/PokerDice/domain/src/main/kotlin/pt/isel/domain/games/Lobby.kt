package pt.isel.domain.games

import pt.isel.domain.users.User

data class Lobby(
    val id: Int,
    val name: String,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val users: List<User>,
    val host: User,
)
