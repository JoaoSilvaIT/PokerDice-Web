package pt.isel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.domain.Sha256TokenEncoder
import pt.isel.domain.TokenExternalInfo
import pt.isel.domain.UsersDomainConfig
import pt.isel.mem.RepositoryUserInMem
import pt.isel.utils.Either
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class UserAuthServiceTest {
    private fun newService(
        repo: RepositoryUserInMem = RepositoryUserInMem(),
        now: Instant = Instant.parse("2025-01-01T00:00:00Z"),
        tokenSizeInBytes: Int = 32,
        tokenTtl: Duration = Duration.ofMinutes(30),
        tokenRollingTtl: Duration = Duration.ofMinutes(15),
        maxTokensPerUser: Int = 3,
    ): Triple<UserAuthService, RepositoryUserInMem, Clock> {
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val service =
            UserAuthService(
                passwordEncoder = BCryptPasswordEncoder(),
                tokenEncoder = Sha256TokenEncoder(),
                config =
                    UsersDomainConfig(
                        tokenSizeInBytes = tokenSizeInBytes,
                        tokenTtl = tokenTtl,
                        tokenRollingTtl = tokenRollingTtl,
                        maxTokensPerUser = maxTokensPerUser,
                    ),
                repoUsers = repo,
                clock = clock,
            )
        return Triple(service, repo, clock)
    }

    @Test
    fun `createUser succeeds and trims input`() {
        val (service, repo, _) = newService()
        val user = service.createUser(" John ", " john@doe.com ", "secret")
        assertEquals("John", user.name)
        assertEquals("john@doe.com", user.email)
        assertNotNull(repo.findByEmail("john@doe.com"))
    }

    @Test
    fun `createUser fails on blank fields`() {
        val (service, _, _) = newService()
        assertThrows(IllegalArgumentException::class.java) { service.createUser("", "x@y", "p") }
        assertThrows(IllegalArgumentException::class.java) { service.createUser("n", "", "p") }
        assertThrows(IllegalArgumentException::class.java) { service.createUser("n", "x@y", "") }
    }

    @Test
    fun `createUser fails if email already exists`() {
        val (service, _, _) = newService()
        service.createUser("John", "john@doe.com", "secret")
        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                service.createUser("Jane", "john@doe.com", "another")
            }
        assertTrue(ex.message!!.contains("Email already in use"))
    }

    @Test
    fun `createToken returns Failure for blank inputs`() {
        val (service, _, _) = newService()
        assertTrue(service.createToken("", "p") is Either.Failure)
        assertTrue(service.createToken("a@b", "") is Either.Failure)
    }

    @Test
    fun `createToken returns Failure for non-existing user or wrong password`() {
        val (service, _, _) = newService()
        // non-existing user
        val r1 = service.createToken("nobody@nowhere", "p")
        assertTrue(r1 is Either.Failure)

        // wrong password
        service.createUser("John", "john@doe.com", "secret")
        val r2 = service.createToken("john@doe.com", "bad")
        assertTrue(r2 is Either.Failure)
    }

    @Test
    fun `createToken returns Success and token authenticates`() {
        val (service, _, _) = newService()
        service.createUser("John", "john@doe.com", "secret")
        val result = service.createToken("john@doe.com", "secret")
        assertTrue(result is Either.Success)
        result as Either.Success<TokenExternalInfo>
        assertNotNull(result.value.tokenValue)

        val user = service.getUserByToken(result.value.tokenValue)
        assertNotNull(user)
        assertEquals("john@doe.com", user!!.email)
    }

    @Test
    fun `getUserByToken returns null for invalid token format`() {
        val (service, _, _) = newService(tokenSizeInBytes = 16)
        // Not a base64url token or wrong length
        val user = service.getUserByToken("not-a-valid-token")
        assertNull(user)
    }

    @Test
    fun `revokeToken returns true only when token existed`() {
        val (service, _, _) = newService()
        service.createUser("John", "john@doe.com", "secret")
        val result = service.createToken("john@doe.com", "secret") as Either.Success<TokenExternalInfo>
        val tokenValue = result.value.tokenValue

        assertTrue(service.revokeToken(tokenValue))
        // second attempt should return false
        assertFalse(service.revokeToken(tokenValue))
    }

    @Test
    fun `token expires by absolute TTL`() {
        val repo = RepositoryUserInMem()
        val t0 = Instant.parse("2025-01-01T00:00:00Z")
        val (serviceAtT0, _, _) =
            newService(
                repo = repo,
                now = t0,
                tokenTtl = Duration.ofSeconds(10),
                tokenRollingTtl = Duration.ofSeconds(100),
            )
        serviceAtT0.createUser("John", "john@doe.com", "secret")
        val token = (serviceAtT0.createToken("john@doe.com", "secret") as Either.Success<TokenExternalInfo>).value.tokenValue

        // Advance beyond absolute TTL
        val (serviceAtT0Plus11, _, _) =
            newService(
                repo = repo,
                now = t0.plusSeconds(11),
                tokenTtl = Duration.ofSeconds(10),
                tokenRollingTtl = Duration.ofSeconds(100),
            )
        val user = serviceAtT0Plus11.getUserByToken(token)
        assertNull(user)
    }

    @Test
    fun `token expires by rolling TTL`() {
        val repo = RepositoryUserInMem()
        val t0 = Instant.parse("2025-01-01T00:00:00Z")
        val (serviceAtT0, _, _) =
            newService(
                repo = repo,
                now = t0,
                tokenTtl = Duration.ofSeconds(1000),
                tokenRollingTtl = Duration.ofSeconds(5),
            )
        serviceAtT0.createUser("John", "john@doe.com", "secret")
        val token = (serviceAtT0.createToken("john@doe.com", "secret") as Either.Success<TokenExternalInfo>).value.tokenValue

        // Advance beyond rolling TTL
        val (serviceAtT0Plus6, _, _) =
            newService(
                repo = repo,
                now = t0.plusSeconds(6),
                tokenTtl = Duration.ofSeconds(1000),
                tokenRollingTtl = Duration.ofSeconds(5),
            )
        val user = serviceAtT0Plus6.getUserByToken(token)
        assertNull(user)
    }
}
