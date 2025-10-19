package pt.isel.domain.games

import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.User
import pt.isel.domain.games.utils.State

data class Game(
    val id: Int,
    val lobbyId: Int,
    val players: Set<PlayerInGame>,
    val numberOfRounds: Int,
    val state: State,
    val currentRound: Round?,
    val startedAt: Long,
    val endedAt: Long?,
    // List to register the gains of each user in each round to decide the final winner
    val gameGains: List<Pair<User, Int>> = emptyList(),
)
 /*
    fun startNewRound(): Game {
        val users = this.lobby.users
        val nextRoundNr = (this.currentRound?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % users.size
        val newRound =
            Round(
                number = nextRoundNr,
                firstPlayerIdx = firstPlayerIndex,
                turn = Turn(users[firstPlayerIndex], Hand(emptyList())),
                users = users,
                userHands = emptyMap(),
            )
        val updatedGame = this.copy(currentRound = newRound)
        return updatedGame
    }

 */
