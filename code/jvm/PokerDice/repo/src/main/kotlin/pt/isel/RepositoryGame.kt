package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.Game
import pt.isel.domain.Lobby

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

    fun findByLobby(lobby: Lobby): Game?

    fun endGame(
        game: Game,
        endedAt: Long,
    ): Game

    fun nextRound(game: Game): Game
}
