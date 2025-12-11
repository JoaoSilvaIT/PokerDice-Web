package pt.isel.domain.games.utils

enum class Face(val strength: Int, val abbreviation: String) {
    ACE(6, "A"),
    KING(5, "K"),
    QUEEN(4, "Q"),
    JACK(3, "J"),
    TEN(2, "T"),
    NINE(1, "9"),
}
