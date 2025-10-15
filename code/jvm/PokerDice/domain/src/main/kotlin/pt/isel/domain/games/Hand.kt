package pt.isel.domain.games

data class Hand(
    val dices: List<Dice>,
) {
    companion object {
        fun rollDices(): Hand {
            val dices = List(5) { Dice.roll() }
            return Hand(dices)
        }
    }
}
