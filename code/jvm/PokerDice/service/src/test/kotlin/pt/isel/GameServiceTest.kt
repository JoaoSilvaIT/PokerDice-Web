package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby
import pt.isel.errors.GameError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@SpringJUnitConfig(TestConfig::class)
class GameServiceTest {
    @Autowired
    private lateinit var serviceGame: GameService

    @Autowired
    private lateinit var serviceLobby: LobbyService

    @Autowired
    private lateinit var serviceUser: UserAuthService

    @Autowired
    private lateinit var trxManager: TransactionManager

    @BeforeEach
    fun setup() {
        trxManager.run {
            repoUsers.clear()
            repoLobby.clear()
            repoGame.clear()
        }
    }

    @Test
    fun `createGame should create and return a game`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val lobby = lobbyResult.value

        val result = serviceGame.createGame(System.currentTimeMillis(), lobby, 5)

        assertIs<Either.Success<Game>>(result)
        assertEquals(5, result.value.numberOfRounds)
        assertEquals(lobby.id, result.value.lobby.id)
    }

    @Test
    fun `createGame fails with invalid number of rounds`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value, 0)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidNumberOfRounds, result.value)
    }

    @Test
    fun `createGame fails with invalid time`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(-1, lobbyResult.value, 5)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidTime, result.value)
    }

    @Test
    fun `createGame by lobbyId should create and return a game`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 5)

        assertIs<Either.Success<Game>>(result)
        assertEquals(5, result.value.numberOfRounds)
    }

    @Test
    fun `createGame by lobbyId fails when lobby not found`() {
        val result = serviceGame.createGame(System.currentTimeMillis(), 999, 5)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidLobby, result.value)
    }

    @Test
    fun `getGame should return game when it exists`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val gameResult = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value, 5)
        assertIs<Either.Success<Game>>(gameResult)

        val game = serviceGame.getGame(gameResult.value.gid)

        assertNotNull(game)
        assertEquals(gameResult.value.gid, game.gid)
    }

    @Test
    fun `getGame should return null when game does not exist`() {
        val game = serviceGame.getGame(999)

        kotlin.test.assertNull(game)
    }

    @Test
    fun `endGame should end a game successfully`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value, 5)
        assertIs<Either.Success<Game>>(gameResult)
        val game = gameResult.value

        val result = serviceGame.endGame(game.gid, startTime + 1000)

        assertIs<Either.Success<Game>>(result)
        assertNotNull(result.value.endedAt)
    }

    @Test
    fun `endGame fails with invalid end time before start time`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value, 5)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.gid, startTime - 1000)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidTime, result.value)
    }

    @Test
    fun `endGame fails when game already ended`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value, 5)
        assertIs<Either.Success<Game>>(gameResult)
        serviceGame.endGame(gameResult.value.gid, startTime + 1000)

        val result = serviceGame.endGame(gameResult.value.gid, startTime + 2000)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.GameAlreadyEnded, result.value)
    }

    @Test
    fun `endGame by gameId should end a game successfully`() {
        val host = serviceUser.createUser("Host", "host@example.com", "password123")
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value, 5)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.gid, startTime + 1000)

        assertIs<Either.Success<Game>>(result)
        assertNotNull(result.value.endedAt)
    }

    @Test
    fun `endGame by gameId fails when game not found`() {
        val result = serviceGame.endGame(999, System.currentTimeMillis())

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.GameNotFound, result.value)
    }
}
