package pt.isel.mem

import pt.isel.RepositoryGame
import pt.isel.RepositoryUser
import pt.isel.domain.games.*
import pt.isel.utils.State
import kotlin.collections.plus

class RepositoryGameInMem : RepositoryGame {
    private val games = mutableListOf<Game>()
    private var game = 0

    override fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game {
        val game = Game(game++, startedAt, null, lobby, numberOfRounds, State.WAITING,null)
        games.add(game)
        return game
    }

    override fun endGame(
        game: Game,
        endedAt: Long,
    ): Game {
        val newGame = Game(game.gid, game.startedAt, endedAt, game.lobby, game.numberOfRounds, State.FINISHED, null)
        save(newGame)
        return newGame
    }

    override fun updateGame(game: Game) {
        val index = games.indexOfFirst { it.gid == game.gid }
        if (index != -1) { // avoids updating non-existing games
            games[index] = game
        }
    }

    override fun startNewRound(game: Game): Game {
        val users = game.lobby.users
        val nextRoundNr = (game.currentRound?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % users.size
        val newRound =
            Round(
                nextRoundNr,
                Turn(users[firstPlayerIndex], Hand(emptyList())),
                users,
                emptyMap(),
            )
        val updatedGame = game.copy(currentRound = nextRoundNr, rounds = game.rounds + newRound, state = State.RUNNING)
        save(updatedGame)
        return updatedGame
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

    override fun nextTurnMem(game: Game): Game {
        val currentRound = game.rounds.find { game.currentRound == it.number }
        requireNotNull(currentRound) { "Round can't be null"}
        val newRound = currentRound.nextTurn()
        val updatedRounds = game.rounds.map { if (it.number == newRound.number) newRound else it }
        val updatedGame = game.copy(rounds = updatedRounds)
        save(updatedGame)
        return updatedGame
    }

    override fun setAnte(game: Game, ante: Int): Game {
        val currentRound = game.rounds.find { game.currentRound == it.number }
        requireNotNull(currentRound) { "Round can't be null"}
        val newRound = currentRound.setAnte(ante)
        val updatedRounds = game.rounds.map { if (it.number == newRound.number) newRound else it }
        val updatedGame = game.copy(rounds = updatedRounds)
        save(updatedGame)
        return updatedGame
    }

    override fun payAnte(game: Game): Game {
        val updatedAnte = game.rounds.find { game.currentRound == it.number }?.payAnte()
        requireNotNull(updatedAnte) { "Round can't be null"}
        val newUsersRound = updatedAnte.payAnte()
        newUsersRound.users.forEach { user -> RepositoryUser.save(user) }
    }
}
