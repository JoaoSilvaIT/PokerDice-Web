package pt.isel.mem

import pt.isel.RepositoryGame
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.domain.games.MAX_ROLLS
import pt.isel.domain.games.MIN_ANTE
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
    ): Game? {
        val threshold = ante ?: MIN_ANTE
        val eligiblePlayers = game.players.filter { it.currentBalance >= threshold }

        if (eligiblePlayers.size < 2) {
            return null
        }

        val nextRoundNr = (game.currentRound?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % eligiblePlayers.size
        val newRound =
            Round(
                number = nextRoundNr,
                firstPlayerIdx = firstPlayerIndex,
                turn =
                    Turn(
                        eligiblePlayers[firstPlayerIndex],
                        MAX_ROLLS,
                        emptyList(),
                    ),
                eligiblePlayers,
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
        chosenDice: List<Dice>,
        round: Round,
    ): Round {
        val currentDice = round.turn.currentDice + chosenDice
        val updatedTurn = round.turn.copy(currentDice = currentDice)
        // In-memory update: find game and update round
        val game = games.find { it.id == round.gameId }
        if (game != null) {
            val updatedRound = round.copy(turn = updatedTurn)
            save(game.copy(currentRound = updatedRound))
            return updatedRound
        }
        return round.copy(turn = updatedTurn)
    }

    override fun distributeWinnings(round: Round): Round {
        // Simple mock implementation: clear pot
        val updatedRound = round.copy(pot = 0)
        // Update game in memory
        val game = games.find { it.id == round.gameId }
        if (game != null) {
            save(game.copy(currentRound = updatedRound))
        }
        return updatedRound
    }

    override fun loadPlayerHands(
        gameId: Int,
        roundNumber: Int,
        players: List<PlayerInGame>,
    ): Map<PlayerInGame, Hand> {
        val game = games.find { it.id == gameId } ?: return emptyMap()
        val round = game.currentRound ?: return emptyMap()
        if (round.number != roundNumber) return emptyMap()
        return round.playerHands
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
