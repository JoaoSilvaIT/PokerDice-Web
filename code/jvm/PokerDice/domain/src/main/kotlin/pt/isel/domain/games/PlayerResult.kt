package pt.isel.domain.games

data class PlayerResult(
    val id: Int,
    val finalBalance: Int,
    val roundsWon: Int,
    val coinsWon: Int,
    val coinsLost: Int,
)
