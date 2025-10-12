package pt.isel.domain.games

import pt.isel.domain.users.User

data class Turn(
    val user: User,
    val hand: Hand?,
) {
    fun rollDices() : Hand {
        val listOfDice = mutableListOf<Dice>()
        val newHand = repeat(5) {
                listOfDice.add(Dice.roll())
            }
        return Hand(listOfDice)
    }
}
