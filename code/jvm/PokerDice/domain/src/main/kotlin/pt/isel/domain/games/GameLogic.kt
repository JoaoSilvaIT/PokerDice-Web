package pt.isel.domain.games

import pt.isel.utils.State

class GameLogic {

    private val games = mutableListOf<Game>()
    private var game = 0

    fun startGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game {
        val game = Game(game++, startedAt, null, lobby, numberOfRounds, State.WAITING, null)
        games.add(game)
        return game
    }

    fun startNewRound(game: Game): Game {

        val users = game.lobby.users
        val nextRoundNr = (game.currentRound?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % users.size

        val newRound = Round(
            nextRoundNr,
            Turn(users[firstPlayerIndex], emptyList()),
            users,
            0
        )

        val updatedGame = game.copy(currentRound = newRound)

        val index = games.indexOfFirst { it.gid == game.gid }

        if (index != -1) {
            games[index] = updatedGame
        }

        return updatedGame
    }
}