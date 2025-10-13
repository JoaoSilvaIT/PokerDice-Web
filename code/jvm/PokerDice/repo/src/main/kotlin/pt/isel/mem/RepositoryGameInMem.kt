package pt.isel.mem

import pt.isel.RepositoryGame
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby
import pt.isel.utils.State

class RepositoryGameInMem : RepositoryGame {
    private val games = mutableListOf<Game>()
    private var game = 0

    override fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game {
        val game = Game(game++, startedAt, null, lobby, numberOfRounds, State.WAITING, null)
        games.add(game)
        return game
    }

    override fun endGame(
        game: Game,
        endedAt: Long,
    ): Game {
        val newGame = Game(game.gid, game.startedAt, endedAt, game.lobby, game.numberOfRounds, State.FINISHED, null)
        return newGame
    }

    override fun updateGame(game: Game) {
        val index = games.indexOf(game)
        games[index] = game
    }

    override fun clear() {
        games.clear()
    }

    override fun deleteById(id: Int) {
        games.removeIf { it.gid == id }
    }

    override fun findAll(): List<Game> {
        return games
    }

    override fun save(entity: Game) {
        games.removeIf { it.gid == entity.gid }
        games.add(entity)
    }

    override fun findById(id: Int): Game? {
        return games.find { it.gid == id }
    }
}
