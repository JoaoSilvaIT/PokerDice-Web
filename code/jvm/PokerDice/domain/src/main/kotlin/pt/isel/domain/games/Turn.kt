package pt.isel.domain.games

const val MAX_ROLLS = 3

data class Turn(
    val player: PlayerInGame,
    val rollsRemaining: Int,
    val currentDice: List<Dice>, // Dados por escolher
    val heldDice: Set<Dice> = emptySet(), // Dados escolhidos 0-4
    val finalHand: Hand = Hand(emptyList()),
)
