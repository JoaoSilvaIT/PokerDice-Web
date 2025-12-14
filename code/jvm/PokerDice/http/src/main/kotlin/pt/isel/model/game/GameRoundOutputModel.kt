package pt.isel.model.game

/**
 * Representation of the current round exposed via HTTP
 */
data class GameRoundOutputModel(
    val number: Int,
    val ante: Int,
    val turnUserId: Int,
    val rollsLeft: Int,
    val currentDice: List<String>,
    val pot: Int,
    val winners: List<PlayerInGameOutputModel>?,
)
