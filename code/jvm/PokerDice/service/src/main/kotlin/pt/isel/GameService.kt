package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby
import pt.isel.errors.GameError
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

@Component
class GameService(
    private val repoGame: RepositoryGame,
) {
    fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Either<GameError, Game> {
        if (numberOfRounds < 1) return failure(GameError.InvalidNumberOfRounds)
        if (repoGame.findByLobby(lobby) != null) return failure(GameError.InvalidLobby)
        if (startedAt <= 0) return failure(GameError.InvalidStartTime)

        return success(repoGame.createGame(startedAt, lobby, numberOfRounds))
    }
}
