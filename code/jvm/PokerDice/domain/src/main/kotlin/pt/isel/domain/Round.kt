package pt.isel.domain

data class Round(
    val numberOfRounds: Int,
    val game: Game,
    val turn: Turn,
)
