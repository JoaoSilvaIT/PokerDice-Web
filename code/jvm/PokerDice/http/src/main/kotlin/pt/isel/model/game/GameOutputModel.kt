package pt.isel.model.game

import pt.isel.domain.games.Game

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
) {
    companion object {
        fun fromDomain(game: Game): GameOutputModel =
            GameOutputModel(
                id = game.gid,
                startedAt = game.startedAt,
                endedAt = game.endedAt,
                lobbyId = game.lobby.id,
                numberOfRounds = game.numberOfRounds,
                state = game.state.name,
                currentRound =
                    game.currentRound?.let { round ->
                        GameRoundOutputModel(
                            number = round.number,
                            ante = round.ante,
                            turnUserId = round.turn.user.id,
                        )
                    },
            )
    }
}
