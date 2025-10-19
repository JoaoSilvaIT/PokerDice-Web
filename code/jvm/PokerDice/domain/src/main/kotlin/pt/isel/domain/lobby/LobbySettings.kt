package pt.isel.domain.lobby

data class LobbySettings(
    val numberOfRounds: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    //val timeout: Long? // Timeout em segundos para iniciar um game com minPlayers
)
