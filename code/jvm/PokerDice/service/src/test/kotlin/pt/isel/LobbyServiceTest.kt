package pt.isel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.Lobby
import pt.isel.domain.PasswordValidationInfo
import pt.isel.domain.User
import pt.isel.errors.LobbyError
import pt.isel.mem.RepositoryLobbyInMem
import pt.isel.utils.Either

class LobbyServiceTest {
    private fun newService(): Pair<LobbyService, RepositoryLobbyInMem> {
        val repo = RepositoryLobbyInMem()
        val service = LobbyService(repoLobby = repo)
        return service to repo
    }

    private fun user(
        id: Int,
        name: String = "User$id",
        email: String = "user$id@example.com",
    ) = User(id, name, email, PasswordValidationInfo("x"))

    @Test
    fun `createLobby fails on blank name`() {
        val (service, _) = newService()
        val host = user(1)
        val result = service.createLobby(host, " ", "desc", maxPlayers = 10, minPlayers = 2)
        assertTrue(result is Either.Failure)
        assertTrue((result as Either.Failure<LobbyError>).value is LobbyError.BlankName)
    }

    @Test
    fun `createLobby fails when minPlayers too low`() {
        val (service, _) = newService()
        val host = user(1)
        val result = service.createLobby(host, "Lobby", "desc", maxPlayers = 10, minPlayers = 1)
        assertTrue(result is Either.Failure)
        assertTrue((result as Either.Failure<LobbyError>).value is LobbyError.MinPlayersTooLow)
    }

    @Test
    fun `createLobby fails when name already used`() {
        val (service, _) = newService()
        val host = user(1)
        val ok = service.createLobby(host, "Poker Room", "desc", maxPlayers = 2, minPlayers = 2)
        assertTrue(ok is Either.Success)

        val dup = service.createLobby(host, "Poker Room", "desc 2", maxPlayers = 2, minPlayers = 2)
        assertTrue(dup is Either.Failure)
        assertTrue((dup as Either.Failure<LobbyError>).value is LobbyError.NameAlreadyUsed)
    }

    @Test
    fun `createLobby success trims fields and includes host as player`() {
        val (service, _) = newService()
        val host = user(42, name = "  Alice  ")
        val created = service.createLobby(host, "  My Lobby  ", "  Something  ", maxPlayers = 2, minPlayers = 2)
        assertTrue(created is Either.Success)
        created as Either.Success<Lobby>
        assertEquals("My Lobby", created.value.name)
        assertEquals("Something", created.value.description)
        assertEquals(1, created.value.users.size)
        assertEquals(host.id, created.value.host.id)
        assertEquals(host.id, created.value.users.first().id)
    }

    @Test
    fun `listVisibleLobbies filters out full lobbies`() {
        val (service, repo) = newService()
        val host = user(1)
        val l1 = (service.createLobby(host, "L1", "", 2, 10) as Either.Success<Lobby>).value
        val l2 = (service.createLobby(host, "L2", "", 2, 10) as Either.Success<Lobby>).value

        // Make L2 full by setting users to maxPlayers
        val fullUsers = (0 until l2.maxPlayers).map { user(100 + it) }
        repo.save(l2.copy(users = fullUsers))

        val visible = service.listVisibleLobbies()
        assertTrue(visible.any { it.id == l1.id })
        assertFalse(visible.any { it.id == l2.id })
    }

    @Test
    fun `joinLobby fails when not found`() {
        val (service, _) = newService()
        val u = user(2)
        val result = service.joinLobby(999, u)
        assertTrue(result is Either.Failure)
        assertTrue((result as Either.Failure<LobbyError>).value is LobbyError.LobbyNotFound)
    }

    @Test
    fun `joinLobby adds user and fails on duplicate join`() {
        val (service, repo) = newService()
        val host = user(1)
        val created = (service.createLobby(host, "Joinable", "", 2, 10) as Either.Success<Lobby>).value
        val bob = user(2)

        val r1 = service.joinLobby(created.id, bob)
        assertTrue(r1 is Either.Success)
        val lobby1 = (r1 as Either.Success<Lobby>).value
        assertTrue(lobby1.users.any { it.id == bob.id })

        // second join should fail with UserAlreadyInLobby
        val r2 = service.joinLobby(created.id, bob)
        assertTrue(r2 is Either.Failure)
        assertTrue((r2 as Either.Failure<LobbyError>).value is LobbyError.UserAlreadyInLobby)

        // repo should still have only one instance of bob
        val fromRepo = repo.findById(created.id)!!
        val bobCount = fromRepo.users.count { it.id == bob.id }
        assertEquals(1, bobCount)
    }

    @Test
    fun `joinLobby fails when full`() {
        val (service, repo) = newService()
        val host = user(1)
        val lobby = (service.createLobby(host, "FullLobby", "", 2, 10) as Either.Success<Lobby>).value
        val filled = lobby.copy(users = (0 until lobby.maxPlayers).map { user(100 + it) })
        repo.save(filled)

        val res = service.joinLobby(lobby.id, user(999))
        assertTrue(res is Either.Failure)
        assertTrue((res as Either.Failure<LobbyError>).value is LobbyError.LobbyFull)
    }

    @Test
    fun `leaveLobby returns Success(false) for non-host and removes user`() {
        val (service, repo) = newService()
        val host = user(1)
        val lobby = (service.createLobby(host, "Leavable", "", 2, 10) as Either.Success<Lobby>).value
        val bob = user(2)
        service.joinLobby(lobby.id, bob)

        val res = service.leaveLobby(lobby.id, bob)
        assertTrue(res is Either.Success)
        assertEquals(false, (res as Either.Success<Boolean>).value)

        val updated = repo.findById(lobby.id)!!
        assertFalse(updated.users.any { it.id == bob.id })
    }

    @Test
    fun `leaveLobby by host closes lobby and returns Success(true)`() {
        val (service, repo) = newService()
        val host = user(1)
        val lobby = (service.createLobby(host, "HostLeaves", "", 2, 10) as Either.Success<Lobby>).value

        val res = service.leaveLobby(lobby.id, host)
        assertTrue(res is Either.Success)
        assertEquals(true, (res as Either.Success<Boolean>).value)
        assertEquals(null, repo.findById(lobby.id))
    }

    @Test
    fun `closeLobby only host can close and lobby must exist`() {
        val (service, repo) = newService()
        val host = user(1)
        val other = user(2)
        val lobby = (service.createLobby(host, "Closable", "", 2, 10) as Either.Success<Lobby>).value

        // not found
        val nf = service.closeLobby(999, host)
        assertTrue(nf is Either.Failure)
        assertTrue((nf as Either.Failure<LobbyError>).value is LobbyError.LobbyNotFound)

        // not host
        val nh = service.closeLobby(lobby.id, other)
        assertTrue(nh is Either.Failure)
        assertTrue((nh as Either.Failure<LobbyError>).value is LobbyError.NotHost)

        // host closes
        val ok = service.closeLobby(lobby.id, host)
        assertTrue(ok is Either.Success)
        assertEquals(null, repo.findById(lobby.id))
    }
}
