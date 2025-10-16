package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby

/**
 * Repository interface for managing games, extends the generic Repository
 */
@Component
interface RepositoryGame : Repository<Game> {
    fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game

    fun updateGame(game: Game)

    fun endGame(
        game: Game,
        endedAt: Long,
    ): Game
}
