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

    @Autowired
    private lateinit var inviteDomain: pt.isel.domain.users.InviteDomain

    @Autowired
    private lateinit var clock: java.time.Clock

    @BeforeEach
    fun setup() {
        trxManager.run {
            repoUsers.clear()
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
    fun `createUser should create and return a user`() {
        val name = "Alice"
        val email = "alice@example.com"
        val password = "securePassword123"
        val invite = createValidInvite()

        val result = serviceUser.createUser(name, email, password, invite)

        assertIs<Either.Success<User>>(result)
        val user = result.value
        assertNotNull(user)
        assertEquals(name, user.name)
        assertEquals(email, user.email)
    }

    @Test
    fun `createUser with already used email should return failure`() {
        val invite1 = createValidInvite()
        serviceUser.createUser("Alice", "alice@example.com", "password123", invite1)

        val invite2 = createValidInvite()
        val result = serviceUser.createUser("Bob", "alice@example.com", "password456", invite2)

        assertIs<Either.Failure<AuthTokenError>>(result)
        assertEquals(AuthTokenError.EmailAlreadyInUse, result.value)
    }

    @Test
    fun `createUser should trim name and email`() {
        val invite = createValidInvite()
        val result = serviceUser.createUser(" John ", " john@doe.com ", "secret", invite)

        assertIs<Either.Success<User>>(result)
        val user = result.value
        assertEquals("John", user.name)
        assertEquals("john@doe.com", user.email)
    }

    @Test
    fun `createToken succeeds with valid credentials`() {
        val invite = createValidInvite()
        serviceUser.createUser("John", "john@doe.com", "mypassword", invite)

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
        val invite = createValidInvite()
        serviceUser.createUser("John", "john@doe.com", "correctpassword", invite)

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
        val invite = createValidInvite()
        val userResult = serviceUser.createUser("John", "john@doe.com", "password", invite)
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
        val invite = createValidInvite()
        serviceUser.createUser("John", "john@doe.com", "password", invite)
        val tokenResult = serviceUser.createToken("john@doe.com", "password")
        assertIs<Either.Success<TokenExternalInfo>>(tokenResult)
        val token = tokenResult.value.tokenValue

        val revoked = serviceUser.revokeToken(token)

        assertTrue(revoked)
        val retrievedUser = serviceUser.getUserByToken(token)
        kotlin.test.assertNull(retrievedUser)
    }
}
