package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import pt.isel.domain.games.Game
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.User
import pt.isel.errors.GameError
import pt.isel.mem.TransactionManagerInMem
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
    private lateinit var trxManager: TransactionManagerInMem

    @Autowired
    private lateinit var inviteDomain: pt.isel.domain.users.InviteDomain

    @Autowired
    private lateinit var clock: java.time.Clock

    @BeforeEach
    fun setup() {
        trxManager.run {
            repoUsers.clear()
            repoLobby.clear()
            repoGame.clear()
            (repoInvite as pt.isel.mem.RepositoryInviteInMem).clear()
        }
    }

    private fun createValidInvite(): String {
        val inviteCode = inviteDomain.generateInviteValue()
        trxManager.run {
            repoInvite.createAppInvite(
                inviterId = 1,
                inviteValidationInfo = inviteDomain.createInviteValidationInformation(inviteCode),
                state = inviteDomain.validState,
                createdAt = clock.instant(),
            )
        }
        return inviteCode
    }

    @Test
    fun `createGame should create and return a game`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val lobby = lobbyResult.value

        val result = serviceGame.createGame(System.currentTimeMillis(), lobby.id, 5, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(5, result.value.numberOfRounds)
        assertEquals(lobby.id, result.value.lobbyId)
    }

    @Test
    fun `createGame fails with invalid number of rounds`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 0, host.id)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidNumberOfRounds, result.value)
    }

    @Test
    fun `createGame fails with invalid time`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(-1, lobbyResult.value.id, 5, host.id)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidTime, result.value)
    }

    @Test
    fun `createGame by lobbyId should create and return a game`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 5, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(5, result.value.numberOfRounds)
    }

    @Test
    fun `createGame by lobbyId fails when lobby not found`() {
        val result = serviceGame.createGame(System.currentTimeMillis(), 999, 5, 1)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.LobbyNotFound, result.value)
    }

    @Test
    fun `getGame should return game when it exists`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val gameResult = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val game = serviceGame.getGame(gameResult.value.id)

        assertNotNull(game)
        assertEquals(gameResult.value.id, game.id)
    }

    @Test
    fun `getGame should return null when game does not exist`() {
        val game = serviceGame.getGame(999)

        kotlin.test.assertNull(game)
    }

    @Test
    fun `endGame should end a game successfully`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)
        val game = gameResult.value

        val result = serviceGame.endGame(game.id, startTime + 1000)

        assertIs<Either.Success<Game>>(result)
        assertNotNull(result.value.endedAt)
    }

    @Test
    fun `endGame fails with invalid end time before start time`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.id, startTime - 1000)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidTime, result.value)
    }

    @Test
    fun `endGame fails when game already ended`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)
        serviceGame.endGame(gameResult.value.id, startTime + 1000)

        val result = serviceGame.endGame(gameResult.value.id, startTime + 2000)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.GameAlreadyEnded, result.value)
    }

    @Test
    fun `endGame by gameId should end a game successfully`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.id, startTime + 1000)

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
