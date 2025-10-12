package pt.isel.mem

import pt.isel.RepositoryGame
import pt.isel.domain.games.Game

class RepositoryGameInMem : RepositoryGame {

    private val games = mutableListOf<Game>()

    override fun findById(id: Int): Game? {
        return games.find { it.gid == id }
    }

    override fun findAll(): List<Game> {
        return games
    }

    override fun save(entity: Game) {
        games.removeIf { it.gid == entity.gid }
        games.add(entity)
    }

    override fun deleteById(id: Int) {
        games.removeIf { it.gid == id }
    }

    override fun clear() {
        games.clear()
    }
}
