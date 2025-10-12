package pt.isel.domain.games

import pt.isel.utils.State

data class Game(
    val gid: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lobby: Lobby,
    val numberOfRounds: Int,
    val state: State,
    val currentRound: Round?,
)
