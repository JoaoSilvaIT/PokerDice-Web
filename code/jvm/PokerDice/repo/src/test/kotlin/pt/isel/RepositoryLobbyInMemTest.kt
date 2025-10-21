package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo
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
        balance: Int = 10,
    ) = User(id, name, email, balance, passwordValidation = PasswordValidationInfo("h"))

    @Test
    fun `createLobby should add lobby with host as first user and allow finding by id and name`() {
        val host = user(1)
        val lobby = repo.createLobby(name = "Poker Room", description = "fun", minPlayers = 2, maxPlayers = 2, host = host)

        assertEquals(0, lobby.id)
        assertEquals("Poker Room", lobby.name)
        assertEquals("fun", lobby.description)
        assertEquals(2, lobby.settings.minPlayers)
        assertEquals(2, lobby.settings.maxPlayers)
        assertEquals(1, lobby.players.size)
        // host in lobby is a UserExternalInfo created from the User passed to createLobby
        assertEquals(host.id, lobby.host.id)
        assertEquals(host.name, lobby.host.name)
        assertEquals(host.id, lobby.players.first().id)

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
        val bobExt = UserExternalInfo(bob.id, bob.name, bob.balance)

        val updated: Lobby = lobby.copy(players = lobby.players + bobExt)
        repo.save(updated)

        val found = repo.findById(lobby.id)
        assertNotNull(found)
        assertEquals(2, found.players.size)
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

    @Test
    fun `createLobby should auto-increment ids`() {
        val host1 = user(1)
        val host2 = user(2)

        val lobby1 = repo.createLobby("L1", "desc1", 2, 4, host1)
        val lobby2 = repo.createLobby("L2", "desc2", 2, 4, host2)
        val lobby3 = repo.createLobby("L3", "desc3", 2, 4, host1)

        assertEquals(0, lobby1.id)
        assertEquals(1, lobby2.id)
        assertEquals(2, lobby3.id)
    }

    @Test
    fun `findAll should return all lobbies`() {
        val host1 = user(1)
        val host2 = user(2)

        repo.createLobby("L1", "d", 2, 4, host1)
        repo.createLobby("L2", "d", 2, 4, host2)
        repo.createLobby("L3", "d", 2, 4, host1)

        val lobbies = repo.findAll()
        assertEquals(3, lobbies.size)
    }

    @Test
    fun `findById should return null for non-existent id`() {
        assertNull(repo.findById(999))
    }

    @Test
    fun `findByName should handle multiple lobbies with different names`() {
        val host = user(1)
        repo.createLobby("Poker Room 1", "d", 2, 4, host)
        repo.createLobby("Poker Room 2", "d", 2, 4, host)

        assertNotNull(repo.findByName("Poker Room 1"))
        assertNotNull(repo.findByName("Poker Room 2"))
        assertNull(repo.findByName("Poker Room 3"))
    }

    @Test
    fun `save should preserve lobby properties when updating`() {
        val host = user(1)
        val lobby = repo.createLobby("Original", "desc", 2, 4, host)

        val player2 = user(2)
        val player2Ext = UserExternalInfo(player2.id, player2.name, player2.balance)
        val updated =
            lobby.copy(
                name = "Updated Name",
                players = lobby.players + player2Ext,
            )
        repo.save(updated)

        val found = repo.findById(lobby.id)
        assertNotNull(found)
        assertEquals("Updated Name", found.name)
        assertEquals(2, found.players.size)
    }

    @Test
    fun `deleteLobbyByHost should only delete lobbies for specific host`() {
        val host1 = user(1)
        val host2 = user(2)
        val host3 = user(3)

        val l1 = repo.createLobby("H1-L1", "d", 2, 4, host1)
        val l2 = repo.createLobby("H2-L1", "d", 2, 4, host2)
        val l3 = repo.createLobby("H3-L1", "d", 2, 4, host3)

        repo.deleteLobbyByHost(host2)

        assertNotNull(repo.findById(l1.id))
        assertNull(repo.findById(l2.id))
        assertNotNull(repo.findById(l3.id))
    }

    @Test
    fun `deleteLobbyByHost should do nothing if host has no lobbies`() {
        val host1 = user(1)
        val host2 = user(2)

        repo.createLobby("L1", "d", 2, 4, host1)
        val sizeBefore = repo.findAll().size

        repo.deleteLobbyByHost(host2) // host2 has no lobbies

        assertEquals(sizeBefore, repo.findAll().size)
    }

    @Test
    fun `deleteById should not affect other lobbies`() {
        val host = user(1)
        val l1 = repo.createLobby("L1", "d", 2, 4, host)
        val l2 = repo.createLobby("L2", "d", 2, 4, host)
        val l3 = repo.createLobby("L3", "d", 2, 4, host)

        repo.deleteById(l2.id)

        assertNotNull(repo.findById(l1.id))
        assertNull(repo.findById(l2.id))
        assertNotNull(repo.findById(l3.id))
    }

    @Test
    fun `createLobby should initialize with correct default values`() {
        val host = user(1)
        val lobby = repo.createLobby("Test Lobby", "A test lobby", 3, 6, host)

        assertEquals("Test Lobby", lobby.name)
        assertEquals("A test lobby", lobby.description)
        assertEquals(3, lobby.settings.minPlayers)
        assertEquals(6, lobby.settings.maxPlayers)
        assertEquals(host.id, lobby.host.id)
        assertEquals(1, lobby.players.size)
        assertTrue(lobby.players.any { it.id == host.id })
    }
}
