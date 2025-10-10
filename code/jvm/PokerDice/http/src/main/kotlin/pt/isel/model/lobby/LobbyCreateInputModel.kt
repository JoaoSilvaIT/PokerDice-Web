package pt.isel.model.lobby

data class LobbyCreateInputModel(
    val name: String,
    val description: String = "",
    val minPlayers: Int,
    val maxPlayers: Int,
)
