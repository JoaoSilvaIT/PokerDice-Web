package pt.isel.model.game

data class PlayerInGameOutputModel(
    val id: Int,
    val name: String,
    val currentBalance: Int,
    val moneyWon: Int,
)

/**
 * Representation of a game exposed via HTTP
 */
data class GameOutputModel(
    val id: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lobbyId: Int?,
    val numberOfRounds: Int,
    val state: String,
    val currentRound: GameRoundOutputModel?,
    val players: List<PlayerInGameOutputModel>,
)
