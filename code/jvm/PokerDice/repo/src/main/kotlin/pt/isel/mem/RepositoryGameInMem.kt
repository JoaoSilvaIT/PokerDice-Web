package pt.isel.mem

import pt.isel.RepositoryGame
import pt.isel.domain.games.*
import pt.isel.domain.games.utils.State
import pt.isel.domain.lobby.Lobby

class RepositoryGameInMem : RepositoryGame {
    private val games = mutableListOf<Game>()
    private var gameIdCounter = 0

    override fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game {
        val players = lobby.players.map {
            PlayerInGame(it.id, it.name, 0, 0)
        }.toSet()
        val game = Game(
            id = gameIdCounter++,
            lobbyId = lobby.id,
            players = players,
            numberOfRounds = numberOfRounds,
            state = State.WAITING,
            currentRound = null,
            startedAt = startedAt,
            endedAt = null
        )
        games.add(game)
        return game
    }

    override fun endGame(
        game: Game,
        endedAt: Long,
    ): Game {
        val newGame = game.copy(endedAt = endedAt, state = State.FINISHED)
        save(newGame)
        return newGame
    }

    override fun clear() {
        games.clear()
    }

    override fun deleteById(id: Int) {
        games.removeIf { it.id == id }
    }

    override fun findAll(): List<Game> {
        return games
    }

    override fun save(entity: Game) {
        games.removeIf { it.id == entity.id }
        games.add(entity)
    }

    override fun findById(id: Int): Game? {
        return games.find { it.id == id }
    }
}
