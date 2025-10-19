package pt.isel.domain.games

const val MAX_ROLLS = 3

data class Turn(
    val player: PlayerInGame,
    val rollsRemaining: Int,
    val currentDice: List<Dice> = emptyList(),
    val finalHand: Hand = Hand(emptyList()),
)
