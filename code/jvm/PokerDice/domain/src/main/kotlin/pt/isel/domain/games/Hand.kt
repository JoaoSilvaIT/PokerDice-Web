package pt.isel.domain.games

const val FINAL_HAND_SIZE = 5

data class Hand(
    val dices: List<Dice>,
)
