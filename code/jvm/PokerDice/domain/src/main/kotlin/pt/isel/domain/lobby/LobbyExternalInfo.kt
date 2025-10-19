package pt.isel.domain.lobby

data class LobbyExternalInfo (
    val name: String,
    val description: String,
    val currentPlayers: Int,
    val numberOfRounds: Int,
)