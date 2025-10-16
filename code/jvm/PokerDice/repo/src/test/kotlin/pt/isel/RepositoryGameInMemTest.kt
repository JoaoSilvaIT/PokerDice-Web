package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Game
import pt.isel.domain.games.Lobby
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.mem.RepositoryGameInMem
import pt.isel.utils.State
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
    ) = User(id = id, name = name, email = email, passwordValidation = PasswordValidationInfo("h"))

    private fun lobby(
        id: Int = 0,
        name: String = "Lobby$id",
        hostId: Int = 100 + id,
        minPlayers: Int = 2,
        maxPlayers: Int = 10,
    ): Lobby {
        val host = user(hostId)
        return Lobby(
            id = id,
            name = name,
            description = "desc",
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            users = listOf(host),
            host = host,
        )
    }

    @Test
    fun `createGame should add game with defaults and be retrievable`() {
        val l = lobby(1)
        val g = repo.createGame(startedAt = 1234L, lobby = l, numberOfRounds = 5)
        assertEquals(0, g.gid)
        assertEquals(1234L, g.startedAt)
        assertNull(g.endedAt)
        assertEquals(l, g.lobby)
        assertEquals(5, g.numberOfRounds)
        assertEquals(State.WAITING, g.state)

        val byId = repo.findById(g.gid)
        assertNotNull(byId)
        assertEquals(g, byId)

        val all = repo.findAll()
        assertTrue(all.isNotEmpty())
    }

    @Test
    fun `endGame should return finished game but not persist until saved`() {
        val l = lobby(3)
        val g = repo.createGame(10L, l, 2)

        val finished: Game = repo.endGame(g, endedAt = 20L)
        assertEquals(g.gid, finished.gid)
        assertEquals(20L, finished.endedAt)
        assertEquals(State.FINISHED, finished.state)

        // repository still has original unsaved state
        val stored = repo.findById(g.gid)!!
        assertNull(stored.endedAt)
        assertEquals(State.WAITING, stored.state)

        // after saving, repo is updated
        repo.save(finished)
        val updated = repo.findById(g.gid)!!
        assertEquals(20L, updated.endedAt)
        assertEquals(State.FINISHED, updated.state)
    }

    @Test
    fun `deleteById should remove game`() {
        val g = repo.createGame(1L, lobby(4), 1)
        assertNotNull(repo.findById(g.gid))
        repo.deleteById(g.gid)
        assertNull(repo.findById(g.gid))
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

        assertEquals(0, g1.gid)
        assertEquals(1, g2.gid)
        assertEquals(2, g3.gid)
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

        val found = repo.findById(game.gid)
        assertNotNull(found)
        assertEquals(State.RUNNING, found.state)

        // Ensure no duplication
        val all = repo.findAll()
        assertEquals(1, all.size)
    }

    @Test
    fun `save should add new game if not exists`() {
        val l = lobby(1)
        val newGame = Game(99, 500L, null, l, 5, State.WAITING, null)
        repo.save(newGame)

        val found = repo.findById(99)
        assertNotNull(found)
        assertEquals(99, found.gid)
    }

    @Test
    fun `updateGame should modify existing game in repository`() {
        val l = lobby(1)
        val game = repo.createGame(100L, l, 3)

        val modified = game.copy(state = State.RUNNING)
        repo.updateGame(modified)

        val found = repo.findById(game.gid)
        assertNotNull(found)
        assertEquals(State.RUNNING, found.state)
    }

    @Test
    fun `updateGame should not throw for non-existent game`() {
        val l = lobby(1)
        val nonExistentGame = Game(999, 100L, null, l, 3, State.WAITING, null)

        // Should not throw
        repo.updateGame(nonExistentGame)

        // Game should not be added by updateGame
        assertNull(repo.findById(999))
    }

    @Test
    fun `deleteById should not affect other games`() {
        val l = lobby(1)
        val g1 = repo.createGame(100L, l, 3)
        val g2 = repo.createGame(200L, l, 4)
        val g3 = repo.createGame(300L, l, 5)

        repo.deleteById(g2.gid)

        assertNotNull(repo.findById(g1.gid))
        assertNull(repo.findById(g2.gid))
        assertNotNull(repo.findById(g3.gid))
    }

    @Test
    fun `endGame should preserve all game properties except endedAt and state`() {
        val l = lobby(1)
        val game = repo.createGame(1000L, l, 10)

        val ended = repo.endGame(game, 2000L)

        assertEquals(game.gid, ended.gid)
        assertEquals(game.startedAt, ended.startedAt)
        assertEquals(2000L, ended.endedAt)
        assertEquals(game.lobby, ended.lobby)
        assertEquals(game.numberOfRounds, ended.numberOfRounds)
        assertEquals(State.FINISHED, ended.state)
        assertNull(ended.currentRoundNumber)
    }

    @Test
    fun `createGame should initialize with WAITING state and null endedAt and currentRound`() {
        val l = lobby(1)
        val game = repo.createGame(5000L, l, 8)

        assertEquals(State.WAITING, game.state)
        assertNull(game.endedAt)
        assertNull(game.currentRoundNumber)
    }

    @Test
    fun `multiple operations should maintain consistency`() {
        val l = lobby(1)
        val g1 = repo.createGame(100L, l, 3)
        val g2 = repo.createGame(200L, l, 4)

        // Update first game
        val updated = g1.copy(state = State.RUNNING)
        repo.save(updated)

        // Delete second game
        repo.deleteById(g2.gid)

        // Create a new game
        val g3 = repo.createGame(300L, l, 5)

        val all = repo.findAll()
        assertEquals(2, all.size)
        assertTrue(all.any { it.gid == g1.gid && it.state == State.RUNNING })
        assertTrue(all.any { it.gid == g3.gid })
        assertTrue(all.none { it.gid == g2.gid })
    }
}
