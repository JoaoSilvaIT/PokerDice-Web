package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby
import pt.isel.errors.GameError
import pt.isel.utils.Either
import pt.isel.utils.State
import pt.isel.utils.failure
import pt.isel.utils.success

@Component
class GameService(
    private val repoGame: RepositoryGame,
    private val repoLobby: RepositoryLobby,
) {
    fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Either<GameError, Game> {
        if (numberOfRounds < 1) return failure(GameError.InvalidNumberOfRounds)
        if (repoLobby.findById(lobby.id) != null) return failure(GameError.InvalidLobby)
        if (startedAt <= 0) return failure(GameError.InvalidTime)

        return success(repoGame.createGame(startedAt, lobby, numberOfRounds))
    }

    fun endGame(
        game: Game,
        endedAt: Long,
    ): Either<GameError, Game> {
        if (endedAt <= 0 || endedAt < game.startedAt) return failure(GameError.InvalidTime)
        if (repoGame.findById(game.gid) != null) return failure(GameError.GameNotFound)

        return success(repoGame.endGame(game, endedAt))
    }
}
