package pt.isel.domain

data class Hand(
    val hand: List<Dice>,
) {
    init {
        require(hand.size == 5) { "The hand must have 5 dices." }
    }
}
