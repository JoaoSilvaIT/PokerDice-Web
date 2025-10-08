package pt.isel.domain

data class Lobby(
    val id: Int,
    val name: String,
    val description: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val host: User,
)
