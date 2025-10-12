package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Lobby
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.mem.RepositoryLobbyInMem
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryLobbyInMemTest {
    private lateinit var repo: RepositoryLobbyInMem

    @BeforeEach
    fun setup() {
        repo = RepositoryLobbyInMem()
    }

    private fun user(
        id: Int,
        name: String = "User$id",
        email: String = "user$id@example.com",
    ) = User(id, name, email, PasswordValidationInfo("h"))

    @Test
    fun `createLobby should add lobby with host as first user and allow finding by id and name`() {
        val host = user(1)
        val lobby = repo.createLobby(name = "Poker Room", description = "fun", minPlayers = 2, maxPlayers = 2, host = host)

        assertEquals(0, lobby.id)
        assertEquals("Poker Room", lobby.name)
        assertEquals("fun", lobby.description)
        assertEquals(2, lobby.minPlayers)
        assertEquals(2, lobby.maxPlayers)
        assertEquals(1, lobby.users.size)
        assertEquals(host, lobby.host)
        assertEquals(host, lobby.users.first())

        // find by id
        val byId = repo.findById(lobby.id)
        assertNotNull(byId)
        assertEquals(lobby, byId)

        // find by name
        val byName = repo.findByName("Poker Room")
        assertNotNull(byName)
        assertEquals(lobby.id, byName.id)
    }

    @Test
    fun `findByName should return null for missing name`() {
        assertNull(repo.findByName("missing"))
    }

    @Test
    fun `save should update existing lobby (e g adding a user) without duplicating`() {
        val host = user(10)
        val lobby = repo.createLobby("L1", "d", 2, 2, host)
        val bob = user(11)

        val updated: Lobby = lobby.copy(users = lobby.users + bob)
        repo.save(updated)

        val found = repo.findById(lobby.id)
        assertNotNull(found)
        assertEquals(2, found.users.size)
        // ensure not duplicated entries
        val all = repo.findAll()
        assertEquals(1, all.size)
        assertEquals(updated, all.first())
    }

    @Test
    fun `deleteById and deleteLobbyById should remove lobby`() {
        val host = user(2)
        val lobby = repo.createLobby("L2", "d", 2, 2, host)
        assertNotNull(repo.findById(lobby.id))

        repo.deleteById(lobby.id)
        assertNull(repo.findById(lobby.id))

        // create again and remove via deleteLobbyById alias
        val lobby2 = repo.createLobby("L3", "d", 2, 2, host)
        assertNotNull(repo.findById(lobby2.id))
        repo.deleteLobbyById(lobby2.id)
        assertNull(repo.findById(lobby2.id))
    }

    @Test
    fun `deleteLobbyByHost should remove all lobbies created by that host`() {
        val host1 = user(1)
        val host2 = user(2)
        val l1 = repo.createLobby("H1-L1", "d", 2, 10, host1)
        val l2 = repo.createLobby("H1-L2", "d", 2, 10, host1)
        val l3 = repo.createLobby("H2-L1", "d", 2, 10, host2)

        assertNotNull(repo.findById(l1.id))
        assertNotNull(repo.findById(l2.id))
        assertNotNull(repo.findById(l3.id))

        repo.deleteLobbyByHost(host1)

        assertNull(repo.findById(l1.id))
        assertNull(repo.findById(l2.id))
        assertNotNull(repo.findById(l3.id))
    }

    @Test
    fun `clear should remove all lobbies`() {
        val host = user(42)
        repo.createLobby("A", "d", 2, 10, host)
        repo.createLobby("B", "d", 2, 10, host)
        assertTrue(repo.findAll().isNotEmpty())

        repo.clear()
        assertTrue(repo.findAll().isEmpty())
    }
}
