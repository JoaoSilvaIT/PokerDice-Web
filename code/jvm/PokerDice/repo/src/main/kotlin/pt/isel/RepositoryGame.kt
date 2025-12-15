package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.Round
import pt.isel.domain.lobby.Lobby

@Component
interface RepositoryGame : Repository<Game> {
    fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game

    fun endGame(
        game: Game,
        endedAt: Long,
    ): Game

    fun updateGameRound(
        round: Round,
        game: Game,
    ): Game

    fun startNewRound(
        game: Game,
        ante: Int?,
    ): Game?

    fun setAnte(
        ante: Int,
        round: Round,
    ): Round

    fun payAnte(round: Round): Round

    fun nextTurn(round: Round): Round

    fun distributeWinnings(round: Round): Round

    fun updateTurn(
        chosenDice: List<Dice>,
        round: Round,
    ): Round

    fun loadPlayerHands(
        gameId: Int,
        roundNumber: Int,
        players: List<PlayerInGame>,
    ): Map<PlayerInGame, Hand>

    fun findActiveGamesByLobbyId(lobbyId: Int): List<Game>
}
