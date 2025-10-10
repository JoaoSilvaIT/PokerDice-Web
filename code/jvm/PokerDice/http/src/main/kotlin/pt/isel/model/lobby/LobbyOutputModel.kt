package pt.isel.model.lobby

data class LobbyOutputModel(
    val id: Int,
    val name: String,
    val description: String?,
    val minPlayers: Int,
    val maxPlayers: Int,
    val players: List<LobbyPlayerOutputModel>,
    val hostId: Int,
)
