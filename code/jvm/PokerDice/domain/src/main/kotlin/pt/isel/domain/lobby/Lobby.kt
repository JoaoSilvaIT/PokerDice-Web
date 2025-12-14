package pt.isel.domain.lobby

import pt.isel.domain.users.UserExternalInfo

data class Lobby(
    val id: Int,
    val name: String,
    val description: String,
    val host: UserExternalInfo,
    val settings: LobbySettings,
    val players: Set<UserExternalInfo>,
    val timeout: Long = 10L,
)
