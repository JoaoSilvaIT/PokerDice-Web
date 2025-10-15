package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby
import pt.isel.errors.GameError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

@Component
class GameService(
    private val trxManager: TransactionManager,
) {
    fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Either<GameError, Game> {
        if (numberOfRounds < 1) return failure(GameError.InvalidNumberOfRounds)
        if (startedAt <= 0) return failure(GameError.InvalidTime)

        return trxManager.run {
            val lobbyExists = repoLobby.findById(lobby.id) != null
            if (!lobbyExists) return@run failure(GameError.InvalidLobby)

            success(repoGame.createGame(startedAt, lobby, numberOfRounds))
        }
    }

    fun createGame(
        startedAt: Long,
        lobbyId: Int,
        numberOfRounds: Int,
    ): Either<GameError, Game> {
        if (numberOfRounds < 1) return failure(GameError.InvalidNumberOfRounds)
        if (startedAt <= 0) return failure(GameError.InvalidTime)

        return trxManager.run {
            val lobby = repoLobby.findById(lobbyId) ?: return@run failure(GameError.InvalidLobby)
            success(repoGame.createGame(startedAt, lobby, numberOfRounds))
        }
    }

    fun getGame(gameId: Int): Game? =
        trxManager.run {
            repoGame.findById(gameId)
        }

    fun endGame(
        game: Game,
        endedAt: Long,
    ): Either<GameError, Game> {
        if (endedAt <= 0 || endedAt < game.startedAt) return failure(GameError.InvalidTime)

        return trxManager.run {
            val currentGame = repoGame.findById(game.gid) ?: return@run failure(GameError.GameNotFound)
            if (currentGame.endedAt != null) return@run failure(GameError.GameAlreadyEnded)
            val endedGame = repoGame.endGame(currentGame, endedAt)
            repoGame.save(endedGame)
            success(endedGame)
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
            val endedGame = repoGame.endGame(game, endedAt)
            repoGame.save(endedGame)
            success(endedGame)
        }
}
