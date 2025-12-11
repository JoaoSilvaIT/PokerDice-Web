package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.MIN_ANTE
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.utils.State
import pt.isel.domain.games.utils.decideGameWinner
import pt.isel.domain.games.utils.decideRoundWinner
import pt.isel.domain.games.utils.rollDicesLogic
import pt.isel.errors.GameError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

@Component
class GameService(
    private val trxManager: TransactionManager,
    private val lobbyEventService: LobbyEventService,
    private val gameEventService: GameEventService,
) {
    fun createGame(
        startedAt: Long,
        lobbyId: Int,
        numberOfRounds: Int,
        creatorId: Int,
    ): Either<GameError, Game> {
        if (numberOfRounds < 1) return failure(GameError.InvalidNumberOfRounds)
        if (startedAt <= 0) return failure(GameError.InvalidTime)
        return trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(GameError.LobbyNotFound)
            if (lobby.host.id != creatorId) return@run failure(GameError.UserNotLobbyHost)
            val game = repoGame.createGame(startedAt, lobby, numberOfRounds)

            lobbyEventService.notifyGameCreated(lobbyId, game.id)

            success(game)
        }
    }

    fun getGame(gameId: Int): Game? =
        trxManager.run {
            repoGame.findById(gameId)
        }

    fun startGame(
        gameId: Int,
        creatorId: Int,
    ): Either<GameError, Game> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            val lobby = repoLobby.findById(game.lobbyId) ?: return@run failure(GameError.LobbyNotFound)

            val activeGames = repoGame.findActiveGamesByLobbyId(game.lobbyId)
            if (activeGames.any { it.id != gameId }) {
                return@run failure(GameError.LobbyHasActiveGame)
            }
            if (lobby.host.id != creatorId) return@run failure(GameError.UserNotLobbyHost)

            val newGame = game.copy(state = State.RUNNING)
            repoGame.save(newGame)

            gameEventService.notifyGameUpdated(gameId)
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
            gameEventService.notifyGameEnded(gameId)
            success(endedGame)
        }

    fun startNewRound(gameId: Int): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound
            if (round != null) {
                if (round.winners.isEmpty() && round.number != 0) return@run failure(GameError.RoundWinnerNotDecided)
            }
            val newGame = repoGame.startNewRound(game)

            gameEventService.notifyGameUpdated(gameId)
            success(newGame)
        }

    fun setAnte(
        gameId: Int,
        ante: Int,
        playerId: Int,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            require(ante > 0 && ante >= MIN_ANTE) { "Cost must be positive and at least $MIN_ANTE" }
            if (round.turn.player.id != playerId) return@run failure(GameError.UserNotFirstPlayerOfRound)
            val newRound = repoGame.setAnte(ante, round)
            val updatedGame = game.copy(currentRound = newRound)
            repoGame.save(updatedGame)

            gameEventService.notifyGameUpdated(gameId)
            success(updatedGame)
        }

    fun nextTurn(
        gameId: Int,
        playerId: Int,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state == State.FINISHED) return@run failure(GameError.GameAlreadyEnded)

            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if (round.turn.currentDice.size < 5) return@run failure(GameError.FinalHandNotValid)
            if (round.turn.player.id != playerId) return@run failure(GameError.UserNotPlayerOfTurn)

            val newRound = repoGame.nextTurn(round)

            // Check if round is complete (nextTurn returned same player, meaning no one else can play)
            val roundComplete = newRound.turn.player.id == round.turn.player.id

            if (roundComplete) {
                // All players have played - determine winner
                val hands = repoGame.loadPlayerHands(game.id, round.number, round.players)
                val winners = decideRoundWinner(round.copy(playerHands = hands))
                val completedRound = round.copy(winners = winners)
                repoGame.save(game.copy(currentRound = completedRound))

                // Distribute winnings FIRST (before checking if game ended)
                val distributedRound = repoGame.distributeWinnings(completedRound)
                val gameAfterDistribution = game.copy(currentRound = distributedRound)
                repoGame.save(gameAfterDistribution)

                gameEventService.notifyRoundEnded(gameId, round.number, winners.first().id)

                // Check if game is finished (after distributing winnings)
                if (round.number >= game.numberOfRounds) {
                    val endedGame = gameAfterDistribution.copy(endedAt = System.currentTimeMillis(), state = State.FINISHED)
                    repoGame.save(endedGame)
                    gameEventService.notifyGameEnded(gameId)
                    return@run success(endedGame)
                } else {
                    // Start new round
                    val newGame = repoGame.startNewRound(gameAfterDistribution)

                    // Set ante for new round (use same ante or increment)
                    val newRoundData = newGame.currentRound ?: return@run failure(GameError.RoundNotStarted)
                    val anteRound = repoGame.setAnte(round.ante, newRoundData)
                    val anteGame = newGame.copy(currentRound = anteRound)
                    repoGame.save(anteGame)

                    // Pay ante
                    val paidRound = repoGame.payAnte(anteRound)
                    val paidGame = anteGame.copy(currentRound = paidRound)
                    repoGame.save(paidGame)

                    gameEventService.notifyGameUpdated(gameId)
                    return@run success(paidGame)
                }
            } else {
                // Round continues with next player
                val updatedGame = game.copy(currentRound = newRound)
                repoGame.save(updatedGame)

                gameEventService.notifyTurnChanged(gameId, newRound.turn.player.id, newRound.number)
                gameEventService.notifyGameUpdated(gameId)
                success(updatedGame)
            }
        }

    fun fold(
        gameId: Int,
        playerId: Int,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)

            // Prevent processing if game already finished
            if (game.state == State.FINISHED) return@run failure(GameError.GameAlreadyEnded)

            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if (round.turn.player.id != playerId) return@run failure(GameError.UserNotPlayerOfTurn)

            val newRound = repoGame.fold(round)

            // Check if only one player remains (others folded)
            val activePlayers =
                newRound.players.filter { player ->
                    !newRound.foldedPlayers.any { it.id == player.id }
                }

            if (activePlayers.size == 1) {
                // Only one player left - they win the round automatically
                val winner = activePlayers.first()
                val completedRound = newRound.copy(winners = listOf(winner))
                repoGame.save(game.copy(currentRound = completedRound))

                // Distribute winnings
                val distributedRound = repoGame.distributeWinnings(completedRound)
                val gameAfterDistribution = game.copy(currentRound = distributedRound)
                repoGame.save(gameAfterDistribution)

                gameEventService.notifyRoundEnded(gameId, round.number, winner.id)

                // Check if game is finished
                if (round.number >= game.numberOfRounds) {
                    val endedGame = gameAfterDistribution.copy(endedAt = System.currentTimeMillis(), state = State.FINISHED)
                    repoGame.save(endedGame)
                    gameEventService.notifyGameEnded(gameId)
                    return@run success(endedGame)
                } else {
                    // Start new round
                    val newGame = repoGame.startNewRound(gameAfterDistribution)
                    val newRoundData = newGame.currentRound ?: return@run failure(GameError.RoundNotStarted)
                    val anteRound = repoGame.setAnte(round.ante, newRoundData)
                    val anteGame = newGame.copy(currentRound = anteRound)
                    repoGame.save(anteGame)
                    val paidRound = repoGame.payAnte(anteRound)
                    val paidGame = anteGame.copy(currentRound = paidRound)
                    repoGame.save(paidGame)

                    gameEventService.notifyGameUpdated(gameId)
                    return@run success(paidGame)
                }
            } else {
                // Game continues with next player
                val updatedGame = game.copy(currentRound = newRound)
                repoGame.save(updatedGame)

                gameEventService.notifyTurnChanged(gameId, newRound.turn.player.id, newRound.number)
                gameEventService.notifyGameUpdated(gameId)
                success(updatedGame)
            }
        }

    fun payAnte(gameId: Int): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if (round.players.any { player -> player.currentBalance < round.ante }) return@run failure(GameError.InsufficientFunds)
            val updatedRound = repoGame.payAnte(round)
            val newGame = game.copy(currentRound = updatedRound)
            repoGame.save(newGame)

            gameEventService.notifyGameUpdated(gameId)
            success(newGame)
        }

    fun updateTurn(
        chosenDice: Dice,
        gameId: Int,
    ): Either<GameError, Game> =
        trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if (round.turn.currentDice.size >= 5) return@run failure(GameError.HandAlreadyFull)

            val updatedRound = repoGame.updateTurn(chosenDice, round)
            val newGame = game.copy(currentRound = updatedRound)
            repoGame.save(newGame)

            gameEventService.notifyDiceRolled(gameId, updatedRound.turn.player.id, updatedRound.turn.currentDice.map { it.face.name })
            gameEventService.notifyGameUpdated(gameId)
            success(newGame)
        }

    fun rollDices(
        gameId: Int,
        playerId: Int,
    ): Either<GameError, List<Dice>> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if (round.turn.player.id != playerId) return@run failure(GameError.UserNotPlayerOfTurn)
            if (round.turn.rollsRemaining <= 0) return@run failure(GameError.NoRollsRemaining)

            val dicesRolled = rollDicesLogic(5 - round.turn.currentDice.size)

            val newTurn = round.turn.copy(rollsRemaining = round.turn.rollsRemaining - 1)
            val newRound = round.copy(turn = newTurn)
            val newGame = game.copy(currentRound = newRound)
            repoGame.save(newGame)

            gameEventService.notifyGameUpdated(gameId)
            success(dicesRolled)
        }
    }

    fun decideRoundWinner(gameId: Int): Either<GameError, List<PlayerInGame>> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)
            if (round.turn.currentDice.size != 5) return@run failure(GameError.FinalHandNotValid)
            if (round.turn.currentDice.size < 5) return@run failure(GameError.FinalHandNotValid)
            val hands = repoGame.loadPlayerHands(game.id, round.number, round.players)
            val winners = decideRoundWinner(round.copy(playerHands = hands))
            val newRound = round.copy(winners = winners)
            repoGame.save(game.copy(currentRound = newRound))

            gameEventService.notifyRoundEnded(gameId, round.number, winners.first().id)
            success(winners)
        }
    }

    fun distributeWinnings(gameId: Int): Either<GameError, List<PlayerInGame>> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.RUNNING) return@run failure(GameError.GameNotStarted)
            val round = game.currentRound ?: return@run failure(GameError.RoundNotStarted)

            val updatedRound = repoGame.distributeWinnings(round)
            repoGame.save(game.copy(currentRound = updatedRound))

            gameEventService.notifyGameUpdated(gameId)
            success(updatedRound.players)
        }
    }

    fun decideGameWinner(gameId: Int): Either<GameError, List<PlayerInGame>> {
        return trxManager.run {
            val game = repoGame.findById(gameId) ?: return@run failure(GameError.GameNotFound)
            if (game.state != State.FINISHED) return@run failure(GameError.GameNotFinished)
            val winners = decideGameWinner(game.players)
            success(winners)
        }
    }
}
