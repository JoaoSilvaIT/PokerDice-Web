package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import pt.isel.domain.users.TokenExternalInfo
import pt.isel.domain.users.User
import pt.isel.errors.AuthTokenError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(TestConfig::class)
class UserAuthServiceTest {
    @Autowired
    private lateinit var serviceUser: UserAuthService

    @Autowired
    private lateinit var trxManager: TransactionManager

    @BeforeEach
    fun setup() {
        trxManager.run { repoUsers.clear() }
    }

    @Test
    fun `createUser should create and return a user`() {
        val name = "Alice"
        val email = "alice@example.com"
        val password = "securePassword123"

        val result = serviceUser.createUser(name, email, password)

        assertIs<Either.Success<User>>(result)
        val user = result.value
        assertNotNull(user)
        assertEquals(name, user.name)
        assertEquals(email, user.email)
    }

    @Test
    fun `createUser with already used email should return failure`() {
        serviceUser.createUser("Alice", "alice@example.com", "password123")

        val result = serviceUser.createUser("Bob", "alice@example.com", "password456")

        assertIs<Either.Failure<AuthTokenError>>(result)
        assertEquals(AuthTokenError.EmailAlreadyInUse, result.value)
    }

    @Test
    fun `createUser should trim name and email`() {
        val result = serviceUser.createUser(" John ", " john@doe.com ", "secret")

        assertIs<Either.Success<User>>(result)
        val user = result.value
        assertEquals("John", user.name)
        assertEquals("john@doe.com", user.email)
    }

    @Test
    fun `createToken succeeds with valid credentials`() {
        serviceUser.createUser("John", "john@doe.com", "mypassword")

        val result = serviceUser.createToken("john@doe.com", "mypassword")

        assertIs<Either.Success<TokenExternalInfo>>(result)
        assertNotNull(result.value.tokenValue)
    }

    @Test
    fun `createToken fails with blank email`() {
        val result = serviceUser.createToken(" ", "password")

        assertIs<Either.Failure<AuthTokenError>>(result)
        assertEquals(AuthTokenError.BlankEmail, result.value)
    }

    @Test
    fun `createToken fails with blank password`() {
        val result = serviceUser.createToken("email@test.com", " ")

        assertIs<Either.Failure<AuthTokenError>>(result)
        assertEquals(AuthTokenError.BlankPassword, result.value)
    }

    @Test
    fun `createToken fails with invalid credentials`() {
        serviceUser.createUser("John", "john@doe.com", "correctpassword")

        val result = serviceUser.createToken("john@doe.com", "wrongpassword")

        assertIs<Either.Failure<AuthTokenError>>(result)
        assertEquals(AuthTokenError.UserNotFoundOrInvalidCredentials, result.value)
    }

    @Test
    fun `createToken fails when user not found`() {
        val result = serviceUser.createToken("nonexistent@test.com", "password")

        assertIs<Either.Failure<AuthTokenError>>(result)
        assertEquals(AuthTokenError.UserNotFoundOrInvalidCredentials, result.value)
    }

    @Test
    fun `getUserByToken returns user when token is valid`() {
        val userResult = serviceUser.createUser("John", "john@doe.com", "password")
        assertIs<Either.Success<User>>(userResult)
        val user = userResult.value
        val tokenResult = serviceUser.createToken("john@doe.com", "password")
        assertIs<Either.Success<TokenExternalInfo>>(tokenResult)
        val token = tokenResult.value.tokenValue

        val retrievedUser = serviceUser.getUserByToken(token)

        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser.id)
        assertEquals(user.email, retrievedUser.email)
    }

    @Test
    fun `getUserByToken returns null when token is invalid`() {
        val retrievedUser = serviceUser.getUserByToken("invalidtoken")

        kotlin.test.assertNull(retrievedUser)
    }

    @Test
    fun `revokeToken removes token`() {
        serviceUser.createUser("John", "john@doe.com", "password")
        val tokenResult = serviceUser.createToken("john@doe.com", "password")
        assertIs<Either.Success<TokenExternalInfo>>(tokenResult)
        val token = tokenResult.value.tokenValue

        val revoked = serviceUser.revokeToken(token)

        assertTrue(revoked)
        val retrievedUser = serviceUser.getUserByToken(token)
        kotlin.test.assertNull(retrievedUser)
    }
}
