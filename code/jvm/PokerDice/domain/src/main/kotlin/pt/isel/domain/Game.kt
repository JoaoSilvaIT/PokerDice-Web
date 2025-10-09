package pt.isel.domain

import pt.isel.utilis.State

data class Game(
    val gid : Int,
    val startedAt : Long,
    val endedAt : Long?,
    val lobby : Lobby,
    val numberOfRounds : Int,
    val state: State
)