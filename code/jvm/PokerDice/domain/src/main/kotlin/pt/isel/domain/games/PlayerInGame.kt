package pt.isel.domain.games

data class PlayerInGame(
    val id: Int,
    val name: String,
    val currentBalance: Int,
    // val isActive: Boolean, // false se não conseguir pagar ante
    val moneyWon: Int,
)
