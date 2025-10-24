package pt.isel.model.game


/**
 * Representation of a game exposed via HTTP
 */
data class GameOutputModel(
    val id: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lobbyId: Int,
    val numberOfRounds: Int,
    val state: String,
    val currentRound: GameRoundOutputModel?,
)