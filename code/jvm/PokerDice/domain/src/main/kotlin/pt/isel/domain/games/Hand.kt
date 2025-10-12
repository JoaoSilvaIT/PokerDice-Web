package pt.isel.domain.games

data class Hand(
    val dices: List<Dice>,
) {
    init {
        require(dices.size == 5) { "The hand must have 5 dices." }
    }
    companion object {
        fun rollDices() : Hand {
            val dices = List(5) { Dice.roll() }
            return Hand(dices)
        }
    }
}
