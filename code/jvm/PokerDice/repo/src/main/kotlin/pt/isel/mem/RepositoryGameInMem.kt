package pt.isel.mem

import pt.isel.RepositoryGame
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.domain.games.MAX_ROLLS
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.games.utils.State
import pt.isel.domain.lobby.Lobby

class RepositoryGameInMem : RepositoryGame {
    private val games = mutableListOf<Game>()
    private var game = 0

    override fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game {
        val playersInTheGame =
            lobby.players.map { user ->
                PlayerInGame(
                    user.id,
                    user.name,
                    user.balance,
                    0,
                )
            }
        val game =
            Game(
                game++,
                lobby.id,
                playersInTheGame,
                numberOfRounds,
                State.WAITING,
                null,
                startedAt,
                null,
            )
        games.add(game)
        return game
    }

    override fun endGame(
        game: Game,
        endedAt: Long,
    ): Game {
        val newGame = game.copy(state = State.TERMINATED, endedAt = endedAt)
        return newGame
    }

    override fun clear() {
        games.clear()
    }

    override fun deleteById(id: Int) {
        games.removeIf { it.id == id }
    }

    override fun findAll(): List<Game> {
        return games
    }

    override fun save(entity: Game) {
        games.removeIf { it.id == entity.id }
        games.add(entity)
    }

    override fun findById(id: Int): Game? {
        return games.find { it.id == id }
    }

    override fun startNewRound(
        game: Game,
        ante: Int?,
    ): Game {
        val nextRoundNr = (game.currentRound?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % game.players.size
        val newRound =
            Round(
                number = nextRoundNr,
                firstPlayerIdx = firstPlayerIndex,
                turn =
                    Turn(
                        game.players[firstPlayerIndex],
                        MAX_ROLLS,
                        emptyList(),
                    ),
                game.players,
                emptyMap(),
                ante = ante ?: 0,
                gameId = game.id,
            )
        val updatedGame = game.copy(currentRound = newRound)
        save(updatedGame)
        return updatedGame
    }

    override fun setAnte(
        ante: Int,
        round: Round,
    ): Round {
        return round.copy(ante = ante)
    }

    override fun payAnte(round: Round): Round {
        val updatedPlayers =
            round.players.map { user ->
                user.copy(currentBalance = user.currentBalance - round.ante)
            }
        return round.copy(players = updatedPlayers, pot = round.pot + (round.ante * round.players.size))
    }

    override fun nextTurn(round: Round): Round {
        val currentPlayerIndex = round.players.indexOfFirst { it.id == round.turn.player.id }
        val nextPlayer = round.players[(currentPlayerIndex + 1) % round.players.size]
        return round.copy(
            turn = Turn(nextPlayer, MAX_ROLLS, emptyList()),
        )
    }

    override fun updateTurn(
        chosenDice: Dice,
        round: Round,
    ): Round {
        TODO("Not yet implemented")
    }

    override fun distributeWinnings(round: Round): Round {
        TODO("Not yet implemented")
    }

    override fun loadPlayerHands(
        gameId: Int,
        roundNumber: Int,
        players: List<PlayerInGame>,
    ): Map<PlayerInGame, Hand> {
        TODO("Not yet implemented")
    }

    override fun updateGameRound(
        round: Round,
        game: Game,
    ): Game {
        val newGame = game.copy(currentRound = round)
        save(newGame)
        return newGame
    }

    override fun findActiveGamesByLobbyId(lobbyId: Int): List<Game> {
        return games.filter { it.lobbyId == lobbyId && it.state == State.RUNNING }
    }
}
