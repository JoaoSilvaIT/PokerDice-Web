package pt.isel.domain.games

import pt.isel.domain.users.User

data class Turn(
    val user: User,
    val hand: Hand = Hand(emptyList()),
) {
    fun rollDices(): Hand {
        val listOfDice = mutableListOf<Dice>()
        repeat(5 - hand.dices.size) {
            listOfDice.add(Dice.roll())
        }
        return Hand(listOfDice)
    }

    fun lockDices(): Hand {
        require(hand.dices.size == 5) { "Hand must have only 5 dices" }
        return hand
    }

    fun chooseDices(dicesToKeep: List<Dice>): Turn {
        return this.copy(hand = Hand(dicesToKeep))
    }
}
