package pt.isel.domain.games

import pt.isel.domain.users.User
import pt.isel.utils.State

data class Game(
    val gid: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val lobby: Lobby,
    val numberOfRounds: Int,
    val state: State,
    val currentRound: Int,
    val rounds : List<Round> = emptyList(),
    // List to register the gains of each user in each round to decide the final winner
    val gameGains: List<Pair<User, Int>> = emptyList(),
) {
    init {
        require(numberOfRounds >= lobby.users.size) {
            "Number of rounds ($numberOfRounds) must be at least equal to the number of players (${lobby.users.size})"
        }
    }
    /*
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
     */
}
