package pt.isel.domain

data class Round(
    // round number in the game
    val number: Int,
    val turn: Turn,
    val users: List<User>,
)
