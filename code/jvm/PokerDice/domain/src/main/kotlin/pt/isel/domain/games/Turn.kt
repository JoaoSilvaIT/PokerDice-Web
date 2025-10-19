package pt.isel.domain.games

import pt.isel.domain.users.UserExternalInfo

data class Turn(
    val player: UserExternalInfo,
    val rollsRemaining: Int,
    val currentDice: List<Dice>, // Dados por escolher
    val heldDice: Set<Dice> = emptySet(), // Dados escolhidos 0-4
    val finalHand: Hand? = null,
) {
    /*

    fun chooseDices(dicesToKeep: List<Dice>): Turn {
        return this.copy(hand = Hand(dicesToKeep))
    }

     */
}
