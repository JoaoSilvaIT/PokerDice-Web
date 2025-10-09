package pt.isel

import pt.isel.domain.Game
import pt.isel.domain.Lobby

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
}
