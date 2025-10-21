package pt.isel.domain.games.utils

enum class HandRank(val strength: Int) {
    FIVE_OF_A_KIND(8),
    FOUR_OF_A_KIND(7),
    FULL_HOUSE(6),
    STRAIGHT(5),
    THREE_OF_A_KIND(4),
    TWO_PAIR(3),
    ONE_PAIR(2),
    HIGH_DICE(1),
}
