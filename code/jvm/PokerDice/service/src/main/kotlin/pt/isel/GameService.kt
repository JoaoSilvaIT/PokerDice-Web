package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Dice
import pt.isel.domain.games.FINAL_HAND_SIZE
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.errors.GameError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import pt.isel.domain.games.utils.State
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.utils.failure
import pt.isel.utils.success
import pt.isel.domain.games.MIN_ANTE

@Component
class GameService(
    private val trxManager: TransactionManager,
) {
    fun createGame(
        startedAt: Long,
        lobbyId: Int,
        numberOfRounds: Int,
    ): Either<GameError, Game> {
        if (numberOfRounds < 1) return failure(GameError.InvalidNumberOfRounds)
        if (startedAt <= 0) return failure(GameError.InvalidTime)
        return trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(GameError.LobbyNotFound)
            success(repoGame.createGame(startedAt, lobby, numberOfRounds))
        }
    }

    fun getGame(gameId: Int): Game? =
        trxManager.run {
            repoGame.findById(gameId)
        }

    fun startGame(
        gameId: Int,
    ): Either<GameError, Game> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            val newGame = game.copy(state = State.RUNNING)
            repoGame.save(newGame)
            success(newGame)
        }
    }

    fun endGame(
        gameId: Int,
        endedAt: Long,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (endedAt <= 0 || endedAt < game.startedAt) return@run failure(GameError.InvalidTime)
            if (game.endedAt != null) return@run failure(GameError.GameAlreadyEnded)
            val endedGame = game.copy(endedAt = endedAt, state = State.FINISHED)
            repoGame.save(endedGame)
            success(endedGame)
        }

    fun startNewRound(gameId: Int): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val newGame = repoGame.startNewRound(game)
            success(newGame)
        }

    fun setAnte(
        gameId: Int,
        ante: Int,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            require(ante > 0 && ante >= MIN_ANTE) { "Cost must be positive and at least $MIN_ANTE" }
            val newRound = repoGame.setAnte(ante, round)
            val updatedGame = game.copy(currentRound = newRound)
            repoGame.save(updatedGame)
            success(updatedGame)
        }

    fun nextTurn(gameId: Int): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)

            val playersList = round.players
            val currentIndex = playersList.indexOfFirst { it.id == round.turn.player.id }
            val updatedPlayerHands = round.playerHands // Final hands should be recorded elsewhere when finalized

            val newRound = if (((currentIndex + 1) % playersList.size) == round.firstPlayerIdx) {
                // End of round, for now just create a new round. Logic should be improved for scoring.
                val nextRoundNumber = (game.currentRound?.number ?: 0) + 1
                val firstPlayerIndex = (nextRoundNumber - 1) % game.players.size
                val firstPlayer = game.players[firstPlayerIndex]
                Round(
                    number = nextRoundNumber,
                    firstPlayerIdx = firstPlayerIndex,
                    turn = Turn(firstPlayer, rollsRemaining = 3, currentDice = emptyList()),
                    players = game.players,
                    playerHands = emptyMap(),
                    gameId = game.id
                )
            } else {
                val nextPlayer = playersList[(currentIndex + 1) % playersList.size]
                round.copy(
                    turn = Turn(nextPlayer, rollsRemaining = 3, currentDice = emptyList()),
                    playerHands = updatedPlayerHands
                )
            }

            val updatedGame = game.copy(currentRound = newRound)
            repoGame.save(updatedGame)
            success(updatedGame)
        }

    fun payAnte(
        gameId: Int
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)

            val updatedRound = repoGame.payAnte(round)
            val newGame = game.copy(currentRound = updatedRound)
            repoGame.save(newGame)
            success(newGame)
        }

    fun updateTurn(
        chosenDice : Dice,
        gameId: Int,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)

            val updatedRound = repoGame.updateTurn(chosenDice, round)
            val newGame = game.copy(currentRound = updatedRound)
            repoGame.save(newGame)
            success(newGame)
        }



}
