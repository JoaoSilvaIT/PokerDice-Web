package pt.isel.domain.games

import pt.isel.domain.users.User

data class Round(
    // round number in the game
    val number: Int,
    val turn: Turn,
    val users: List<User>,
    val price: Int
)
