package pt.isel

import org.junit.jupiter.api.BeforeEach
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.Token
import pt.isel.domain.users.TokenValidationInfo
import pt.isel.mem.RepositoryUserInMem
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryUserInMemTest {
    private lateinit var repo: RepositoryUserInMem

    @BeforeEach
    fun setup() {
        repo = RepositoryUserInMem()
    }

    @Test
    fun `createUser should add and retrieve user`() {
        val pwdInfo = PasswordValidationInfo("hash123")
        val user = repo.createUser("Alice", "alice@example.com", pwdInfo)

        val found = repo.findByEmail("alice@example.com")
        assertNotNull(found)
        assertEquals(user, found)
        assertEquals(0, user.id)
    }

    @Test
    fun `findByEmail should return null if not found`() {
        assertNull(repo.findByEmail("missing@example.com"))
    }

    @Test
    fun `findById should return correct user`() {
        val u2 = repo.createUser("B", "b@x.com", PasswordValidationInfo("h2"))
        assertEquals(u2, repo.findById(u2.id))
        assertNull(repo.findById(99))
    }

    @Test
    fun `createToken should add token and enforce maxTokens limit`() {
        val user = repo.createUser("Bob", "bob@example.com", PasswordValidationInfo("h"))
        val t1 = Token(TokenValidationInfo("t1"), user.id, Instant.now(), Instant.now())
        val t2 = Token(TokenValidationInfo("t2"), user.id, Instant.now(), Instant.now())
        val t3 = Token(TokenValidationInfo("t3"), user.id, Instant.now(), Instant.now())

        repo.createToken(t1, maxTokens = 2)
        repo.createToken(t2, maxTokens = 2)
        repo.createToken(t3, maxTokens = 2) // deve remover o mais antigo (t1)

        val result = repo.getTokenByTokenValidationInfo(TokenValidationInfo("t1"))
        assertNull(result) // t1 removido

        assertNotNull(repo.getTokenByTokenValidationInfo(TokenValidationInfo("t2")))
        assertNotNull(repo.getTokenByTokenValidationInfo(TokenValidationInfo("t3")))
    }

    @Test
    fun `updateTokenLastUsed should replace old token`() {
        val user = repo.createUser("Bob", "bob@example.com", PasswordValidationInfo("h"))
        val t1 = Token(TokenValidationInfo("t1"), user.id, Instant.now(), Instant.now())

        repo.createToken(t1, maxTokens = 3)

        val updated = t1.copy(lastUsedAt = Instant.now().plusSeconds(60))
        repo.updateTokenLastUsed(updated, Instant.now())

        val (usr, token) = repo.getTokenByTokenValidationInfo(TokenValidationInfo("t1"))!!
        assertEquals(updated.lastUsedAt, token.lastUsedAt)
        assertEquals(user, usr)
    }

    @Test
    fun `removeTokenByValidationInfo should remove and return count`() {
        val user = repo.createUser("Bob", "bob@example.com", PasswordValidationInfo("h"))
        val t1 = Token(TokenValidationInfo("tok1"), user.id, Instant.now(), Instant.now())
        repo.createToken(t1, maxTokens = 3)

        val count = repo.removeTokenByValidationInfo(TokenValidationInfo("tok1"))
        assertEquals(1, count)
        assertNull(repo.getTokenByTokenValidationInfo(TokenValidationInfo("tok1")))
    }

    @Test
    fun `save should update existing user`() {
        val user = repo.createUser("Alice", "alice@example.com", PasswordValidationInfo("h"))
        val updated = user.copy(name = "Alice Updated")
        repo.save(updated)

        val found = repo.findById(user.id)
        assertEquals("Alice Updated", found?.name)
    }

    @Test
    fun `deleteById should remove user`() {
        val user = repo.createUser("Alice", "alice@example.com", PasswordValidationInfo("h"))
        repo.deleteById(user.id)
        assertNull(repo.findById(user.id))
    }

    @Test
    fun `clear should remove all users and tokens`() {
        val user = repo.createUser("Alice", "alice@example.com", PasswordValidationInfo("h"))
        val token = Token(TokenValidationInfo("t"), user.id, Instant.now(), Instant.now())
        repo.createToken(token, 3)

        repo.clear()

        assertTrue(repo.findAll().isEmpty())
        assertNull(repo.getTokenByTokenValidationInfo(TokenValidationInfo("t")))
    }
}
