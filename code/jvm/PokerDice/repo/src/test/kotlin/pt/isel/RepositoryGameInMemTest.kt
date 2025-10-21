package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Game
import pt.isel.domain.games.utils.State
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.MIN_BALANCE
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.mem.RepositoryGameInMem
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryGameInMemTest {
    private lateinit var repo: RepositoryGameInMem

    @BeforeEach
    fun setup() {
        repo = RepositoryGameInMem()
    }

    private fun user(
        id: Int,
        name: String = "User$id",
        email: String = "user$id@example.com",
    ) = User(id = id, name = name, email = email, balance = MIN_BALANCE, passwordValidation = PasswordValidationInfo("h"))

    private fun lobby(
        id: Int = 0,
        name: String = "Lobby$id",
        hostId: Int = 100 + id,
        minPlayers: Int = 2,
        maxPlayers: Int = 10,
    ): Lobby {
        val host = user(hostId)
        val hostInfo = pt.isel.domain.users.UserExternalInfo(host.id, host.name, host.balance)
        return Lobby(
            id = id,
            name = name,
            description = "desc",
            host = hostInfo,
            settings =
                pt.isel.domain.lobby.LobbySettings(
                    numberOfRounds = 3,
                    minPlayers = minPlayers,
                    maxPlayers = maxPlayers,
                ),
            players = setOf(hostInfo),
        )
    }

    @Test
    fun `createGame should add game with defaults and be retrievable`() {
        val l = lobby(1)
        val g = repo.createGame(startedAt = 1234L, lobby = l, numberOfRounds = 5)
        assertEquals(0, g.id)
        assertEquals(1234L, g.startedAt)
        assertNull(g.endedAt)
        assertEquals(l.id, g.lobbyId)
        assertEquals(5, g.numberOfRounds)
        assertEquals(State.WAITING, g.state)

        val byId = repo.findById(g.id)
        assertNotNull(byId)
        assertEquals(g, byId)

        val all = repo.findAll()
        assertTrue(all.isNotEmpty())
    }

    @Test
    fun `endGame should return terminated game but not persist until saved`() {
        val l = lobby(3)
        val g = repo.createGame(10L, l, 2)

        val finished: Game = repo.endGame(g, endedAt = 20L)
        assertEquals(g.id, finished.id)
        assertEquals(20L, finished.endedAt)
        // implementation uses TERMINATED when ending a game
        assertEquals(State.TERMINATED, finished.state)

        // repository still has original unsaved state
        val stored = repo.findById(g.id)!!
        assertNull(stored.endedAt)
        assertEquals(State.WAITING, stored.state)

        // after saving, repo is updated
        repo.save(finished)
        val updated = repo.findById(g.id)!!
        assertEquals(20L, updated.endedAt)
        assertEquals(State.TERMINATED, updated.state)
    }

    @Test
    fun `deleteById should remove game`() {
        val g = repo.createGame(1L, lobby(4), 1)
        assertNotNull(repo.findById(g.id))
        repo.deleteById(g.id)
        assertNull(repo.findById(g.id))
    }

    @Test
    fun `clear should remove all games`() {
        repo.createGame(1L, lobby(5), 1)
        repo.createGame(2L, lobby(6), 1)
        assertTrue(repo.findAll().isNotEmpty())
        repo.clear()
        assertTrue(repo.findAll().isEmpty())
    }

    @Test
    fun `createGame should auto-increment ids`() {
        val l1 = lobby(1)
        val l2 = lobby(2)
        val l3 = lobby(3)

        val g1 = repo.createGame(100L, l1, 3)
        val g2 = repo.createGame(200L, l2, 4)
        val g3 = repo.createGame(300L, l3, 5)

        assertEquals(0, g1.id)
        assertEquals(1, g2.id)
        assertEquals(2, g3.id)
    }

    @Test
    fun `findById should return null for non-existent id`() {
        assertNull(repo.findById(999))
    }

    @Test
    fun `findAll should return all games`() {
        val l = lobby(1)
        repo.createGame(100L, l, 3)
        repo.createGame(200L, l, 4)
        repo.createGame(300L, l, 5)

        val games = repo.findAll()
        assertEquals(3, games.size)
    }

    @Test
    fun `save should update existing game without duplicating`() {
        val l = lobby(1)
        val game = repo.createGame(100L, l, 3)

        val updated = game.copy(state = State.RUNNING)
        repo.save(updated)

        val found = repo.findById(game.id)
        assertNotNull(found)
        assertEquals(State.RUNNING, found.state)

        // Ensure no duplication
        val all = repo.findAll()
        assertEquals(1, all.size)
    }

    @Test
    fun `save should add new game if not exists`() {
        val l = lobby(1)
        val newGame =
            Game(
                id = 99,
                lobbyId = l.id,
                players = emptyList(),
                numberOfRounds = 5,
                state = State.WAITING,
                currentRound = null,
                startedAt = 500L,
                endedAt = null,
            )
        repo.save(newGame)

        val found = repo.findById(99)
        assertNotNull(found)
        assertEquals(99, found.id)

        // cleanup
        repo.deleteById(99)
        assertNull(repo.findById(99))
    }

    @Test
    fun `createGame should initialize with WAITING state and null endedAt and currentRound`() {
        val l = lobby(1)
        val game = repo.createGame(5000L, l, 8)

        assertEquals(State.WAITING, game.state)
        assertNull(game.endedAt)
        assertNull(game.currentRound)
    }

    @Test
    fun `multiple operations should maintain consistency`() {
        val l = lobby(1)
        val g1 = repo.createGame(100L, l, 3)
        val g2 = repo.createGame(200L, l, 4)
        val g3 = repo.createGame(300L, l, 5)

        // Delete middle game
        repo.deleteById(g2.id)

        // Update first game
        val updated = g1.copy(state = State.RUNNING)
        repo.save(updated)

        val all = repo.findAll()
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == g1.id && it.state == State.RUNNING })
        assertTrue(all.any { it.id == g3.id })
        assertTrue(all.none { it.id == g2.id })
    }
}
