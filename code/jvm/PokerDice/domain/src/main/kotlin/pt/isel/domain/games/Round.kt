package pt.isel.domain.games

import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo

const val MIN_ANTE = 10

data class Round(
    // round number in the game
    val number: Int,
    val firstPlayerIdx: Int,
    val turn: Turn,
    val players: List<PlayerInGame>,
    val playerHands: Map<PlayerInGame, Hand>,
    val ante: Int = MIN_ANTE,
    val pot : Int = 0,
    val winners: List<PlayerInGame> = emptyList(),
    val gameId : Int,
)
