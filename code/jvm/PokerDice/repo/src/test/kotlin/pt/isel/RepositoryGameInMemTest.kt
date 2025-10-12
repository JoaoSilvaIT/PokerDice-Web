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
    ) = User(id = id, name = name, email = email, passwordValidation =  PasswordValidationInfo("h"))

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
}
