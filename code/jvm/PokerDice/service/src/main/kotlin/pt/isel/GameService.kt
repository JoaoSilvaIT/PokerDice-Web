package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.*
import pt.isel.errors.GameError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import pt.isel.domain.games.utils.State
import pt.isel.domain.games.utils.decideRoundWinner
import pt.isel.domain.games.utils.decideGameWinner
import pt.isel.utils.failure
import pt.isel.utils.success
import pt.isel.domain.games.utils.rollDicesLogic


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

            val activeGames = repoGame.findActiveGamesByLobbyId(game.lobbyId)
            if (activeGames.any { it.id != gameId }) {
                return@run failure(GameError.LobbyHasActiveGame)
            }

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
            val newRound = repoGame.nextTurn(round)
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
            if(round.turn.currentDice.size >= 5) return@run failure(GameError.HandAlreadyFull)

            val updatedRound = repoGame.updateTurn(chosenDice, round)
            val newGame = game.copy(currentRound = updatedRound)
            repoGame.save(newGame)
            success(newGame)
        }

    fun rollDices(gameId: Int): Either<GameError, List<Dice>> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            val dicesRolled = rollDicesLogic(5 - round.turn.currentDice.size)
            success(dicesRolled)
        }
    }

    fun decideRoundWinner(gameId: Int): Either<GameError, List<PlayerInGame>>{
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if(round.turn.currentDice.size != 5) return@run failure(GameError.FinalHandNotValid)
            val hands = repoGame.loadPlayerHands(game.id, round.number, round.players)
            val winners = decideRoundWinner(round.copy(playerHands = hands))
            val newRound = round.copy(winners = winners)
            repoGame.save(game.copy(currentRound = newRound))
            success(winners)
        }
    }

    fun distributeWinnings(gameId: Int): Either<GameError, List<PlayerInGame>>{
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)

            val updatedRound = repoGame.distributeWinnings(round)
            repoGame.save(game.copy(currentRound = updatedRound))
            success(updatedRound.players)
        }
    }

    fun decideGameWinner(gameId: Int): Either<GameError, List<PlayerInGame>>{
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.FINISHED) return@run failure(GameError.GameNotFinished)
            val winners = decideGameWinner(game.players)
            success(winners)
        }
    }

}
