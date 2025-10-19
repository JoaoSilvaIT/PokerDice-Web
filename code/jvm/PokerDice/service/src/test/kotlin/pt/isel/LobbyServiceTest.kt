package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.User
import pt.isel.errors.LobbyError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(TestConfig::class)
class LobbyServiceTest {
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
        }
    }

    @Test
    fun `createLobby should create and return a lobby`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val result = serviceLobby.createLobby(host, "Poker Room", "Fun game", 2, 4)

        assertIs<Either.Success<Lobby>>(result)
        assertEquals("Poker Room", result.value.name)
        assertEquals("Fun game", result.value.description)
        assertEquals(2, result.value.settings.minPlayers)
        assertEquals(4, result.value.settings.maxPlayers)
        assertEquals(host.id, result.value.host.id)
    }

    @Test
    fun `createLobby fails on blank name`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val result = serviceLobby.createLobby(host, " ", "desc", 2, 10)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.BlankName, result.value)
    }

    @Test
    fun `createLobby fails when minPlayers too low`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val result = serviceLobby.createLobby(host, "Lobby", "desc", 1, 10)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.MinPlayersTooLow, result.value)
    }

    @Test
    fun `createLobby fails when maxPlayers too high`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val result = serviceLobby.createLobby(host, "Lobby", "desc", 2, 11)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.MaxPlayersTooHigh, result.value)
    }

    @Test
    fun `createLobby fails when minPlayers greater than maxPlayers`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val result = serviceLobby.createLobby(host, "Lobby", "desc", 5, 3)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.MinPlayersTooLow, result.value)
    }

    @Test
    fun `createLobby fails when name already used`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        serviceLobby.createLobby(host, "Poker Room", "desc", 2, 2)

        val result = serviceLobby.createLobby(host, "Poker Room", "desc 2", 2, 2)

        assertIs<Either.Failure<LobbyError>>(result)
        assertIs<LobbyError.NameAlreadyUsed>(result.value)
    }

    @Test
    fun `createLobby trims name and description`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val result = serviceLobby.createLobby(host, " Poker Room ", " Fun game ", 2, 4)

        assertIs<Either.Success<Lobby>>(result)
        assertEquals("Poker Room", result.value.name)
        assertEquals("Fun game", result.value.description)
    }

    @Test
    fun `listVisibleLobbies returns only lobbies with available slots`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val playerResult = serviceUser.createUser("Player", "player@example.com", "password123")
        assertIs<Either.Success<User>>(playerResult)
        val player = playerResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Full Lobby", "desc", 2, 2)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        serviceLobby.joinLobby(lobbyResult.value.id, player)

        serviceLobby.createLobby(host, "Available Lobby", "desc", 2, 4)

        val visible = serviceLobby.listVisibleLobbies()

        assertEquals(1, visible.size)
        assertEquals("Available Lobby", visible[0].name)
    }

    @Test
    fun `joinLobby should add user to lobby`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val playerResult = serviceUser.createUser("Player", "player@example.com", "password123")
        assertIs<Either.Success<User>>(playerResult)
        val player = playerResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceLobby.joinLobby(lobbyResult.value.id, player)

        assertIs<Either.Success<Lobby>>(result)
        assertTrue(result.value.players.any { it.id == player.id })
    }

    @Test
    fun `joinLobby fails when lobby is full`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val player1Result = serviceUser.createUser("Player1", "player1@example.com", "password123")
        assertIs<Either.Success<User>>(player1Result)
        val player1 = player1Result.value
        val player2Result = serviceUser.createUser("Player2", "player2@example.com", "password123")
        assertIs<Either.Success<User>>(player2Result)
        val player2 = player2Result.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 2)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        serviceLobby.joinLobby(lobbyResult.value.id, player1)

        val result = serviceLobby.joinLobby(lobbyResult.value.id, player2)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.LobbyFull, result.value)
    }

    @Test
    fun `joinLobby fails when user already in lobby`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val playerResult = serviceUser.createUser("Player", "player@example.com", "password123")
        assertIs<Either.Success<User>>(playerResult)
        val player = playerResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        serviceLobby.joinLobby(lobbyResult.value.id, player)

        val result = serviceLobby.joinLobby(lobbyResult.value.id, player)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.UserAlreadyInLobby, result.value)
    }

    @Test
    fun `leaveLobby should remove user from lobby`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val playerResult = serviceUser.createUser("Player", "player@example.com", "password123")
        assertIs<Either.Success<User>>(playerResult)
        val player = playerResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        serviceLobby.joinLobby(lobbyResult.value.id, player)

        val result = serviceLobby.leaveLobby(lobbyResult.value.id, player)

        assertIs<Either.Success<Boolean>>(result)
        assertEquals(false, result.value)

        val lobby = serviceLobby.findLobbyById(lobbyResult.value.id)
        assertNotNull(lobby)
        assertTrue(lobby.players.none { it.id == player.id })
    }

    @Test
    fun `leaveLobby deletes lobby when host leaves`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceLobby.leaveLobby(lobbyResult.value.id, host)

        assertIs<Either.Success<Boolean>>(result)
        assertEquals(true, result.value)

        val lobby = serviceLobby.findLobbyById(lobbyResult.value.id)
        kotlin.test.assertNull(lobby)
    }

    @Test
    fun `closeLobby should delete lobby when called by host`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceLobby.closeLobby(lobbyResult.value.id, host)

        assertIs<Either.Success<Unit>>(result)

        val lobby = serviceLobby.findLobbyById(lobbyResult.value.id)
        kotlin.test.assertNull(lobby)
    }

    @Test
    fun `closeLobby fails when user is not the host`() {
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123")
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val playerResult = serviceUser.createUser("Player", "player@example.com", "password123")
        assertIs<Either.Success<User>>(playerResult)
        val player = playerResult.value

        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceLobby.closeLobby(lobbyResult.value.id, player)

        assertIs<Either.Failure<LobbyError>>(result)
        assertEquals(LobbyError.NotHost, result.value)
    }
}
