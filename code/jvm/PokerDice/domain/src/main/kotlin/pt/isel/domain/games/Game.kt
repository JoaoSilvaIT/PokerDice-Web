package pt.isel.domain.games

import pt.isel.utils.State

data class Game(
    val gid: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lobby: Lobby,
    val numberOfRounds: Int,
    val state: State,
    val currentRound: Round?,
) {
    init {
        require(numberOfRounds >= lobby.users.size) {
            "Number of rounds ($numberOfRounds) must be at least equal to the number of players (${lobby.users.size})"
        }
    }

    fun startNewRound(game: Game): Game {
        val users = game.lobby.users
        val nextRoundNr = (game.currentRound?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % users.size

        val newRound =
            Round(
                nextRoundNr,
                Turn(users[firstPlayerIndex], Hand(emptyList())),
                users,
                emptyMap(),
            )

        val updatedGame = game.copy(currentRound = newRound, state = State.RUNNING)

        return updatedGame
    }
}
