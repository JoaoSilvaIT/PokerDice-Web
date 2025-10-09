package pt.isel

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pt.isel.domain.PasswordValidationInfo
import pt.isel.domain.Token
import pt.isel.domain.TokenEncoder
import pt.isel.domain.TokenExternalInfo
import pt.isel.domain.User
import pt.isel.domain.UsersDomainConfig
import pt.isel.utilis.Either
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64.getUrlDecoder
import java.util.Base64.getUrlEncoder

@Component
class UserAuthService(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
    private val repoUsers: RepositoryUser,
    private val clock: Clock,
) {
    private fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ) = passwordEncoder.matches(
        password,
        validationInfo.validationInfo,
    )

    private fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(
            validationInfo = passwordEncoder.encode(password),
        )

    fun createUser(
        name: String,
        email: String,
        password: String,
    ): User {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }

        val emailTrimmed = email.trim()
        require(repoUsers.findByEmail(emailTrimmed) == null) { "Email already in use" }

        val passwordValidationInfo = createPasswordValidationInformation(password)
        return repoUsers.createUser(name.trim(), emailTrimmed, passwordValidationInfo)
    }

    fun createToken(
        email: String,
        password: String,
    ): Either<AuthTokenError, TokenExternalInfo> { // Replaced by Either
        if (email.isBlank()) return Either.Failure(AuthTokenError.BlankEmail)
        if (password.isBlank()) return Either.Failure(AuthTokenError.BlankPassword)

        val user =
            repoUsers.findByEmail(email.trim())
                ?: return Either.Failure(AuthTokenError.UserNotFoundOrInvalidCredentials)

        if (!validatePassword(password, user.passwordValidation)) {
            return Either.Failure(AuthTokenError.UserNotFoundOrInvalidCredentials)
        }
        val tokenValue = generateTokenValue()
        val now = clock.instant()
        val newToken =
            Token(
                tokenEncoder.createValidationInformation(tokenValue),
                user.id,
                createdAt = now,
                lastUsedAt = now,
            )
        repoUsers.createToken(newToken, config.maxTokensPerUser)
        return Either.Success(
            TokenExternalInfo(
                tokenValue,
                getTokenExpiration(newToken),
            ),
        )
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        val removed = repoUsers.removeTokenByValidationInfo(tokenValidationInfo)
        return removed > 0
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token)) {
            return null
        }
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)

        val userAndToken: Pair<User, Token>? = repoUsers.getTokenByTokenValidationInfo(tokenValidationInfo)
        return if (userAndToken != null && isTokenTimeValid(clock, userAndToken.second)) {
            repoUsers.updateTokenLastUsed(userAndToken.second, clock.instant())
            userAndToken.first
        } else {
            null
        }
    }

    private fun canBeToken(token: String): Boolean =
        try {
            getUrlDecoder().decode(token).size == config.tokenSizeInBytes
        } catch (ex: IllegalArgumentException) {
            false
        }

    private fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.instant()
        return token.createdAt <= now &&
            Duration.between(token.createdAt, now) <= config.tokenTtl &&
            Duration.between(token.lastUsedAt, now) <= config.tokenRollingTtl
    }

    private fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            getUrlEncoder().encodeToString(byteArray)
        }

    private fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }
}
