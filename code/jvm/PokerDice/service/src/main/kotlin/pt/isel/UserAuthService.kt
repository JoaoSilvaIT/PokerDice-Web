package pt.isel

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pt.isel.domain.users.InviteDomain
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.Token
import pt.isel.domain.users.TokenEncoder
import pt.isel.domain.users.TokenExternalInfo
import pt.isel.domain.users.User
import pt.isel.domain.users.UsersDomainConfig
import pt.isel.errors.AuthTokenError
import pt.isel.repo.TransactionManager
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success
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
    private val inviteDomain: InviteDomain,
    private val trxManager: TransactionManager,
    private val clock: Clock,
) {
    fun createUser(
        name: String,
        email: String,
        password: String,
        invite: String,
    ): Either<AuthTokenError, User> {
        if (name.isBlank()) return failure(AuthTokenError.BlankName)
        if (email.isBlank()) return failure(AuthTokenError.BlankEmail)
        if (password.isBlank()) return failure(AuthTokenError.BlankPassword)
        if (invite.isBlank()) return failure(AuthTokenError.BlankInvite)

        val emailTrimmed = email.trim()

        return trxManager.run {
            if (repoUsers.findByEmail(emailTrimmed) != null) {
                return@run failure(AuthTokenError.EmailAlreadyInUse)
            }
            val passwordValidationInfo = createPasswordValidationInformation(password)
            val inviteValidationInfo = inviteDomain.createInviteValidationInformation(invite)
            val invite = repoInvite.getAppInviteByValidationInfo(inviteValidationInfo)
            if (invite == null || !inviteDomain.isInviteCodeValid(invite.state)) {
                return@run failure(AuthTokenError.InvalidInvite)
            }
            val newUser = repoUsers.createUser(name.trim(), emailTrimmed, passwordValidationInfo)
            repoInvite.changeInviteState(invite.id, inviteDomain.usedState)
            success(newUser)
        }
    }

    fun createToken(
        email: String,
        password: String,
    ): Either<AuthTokenError, TokenExternalInfo> {
        if (email.isBlank()) return failure(AuthTokenError.BlankEmail)
        if (password.isBlank()) return failure(AuthTokenError.BlankPassword)

        return trxManager.run {
            val user =
                repoUsers.findByEmail(email.trim())
                    ?: return@run failure(AuthTokenError.UserNotFoundOrInvalidCredentials)

            if (!validatePassword(password, user.passwordValidation)) {
                return@run failure(AuthTokenError.UserNotFoundOrInvalidCredentials)
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

            success(TokenExternalInfo(tokenValue, getTokenExpiration(newToken)))
        }
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
        return trxManager.run {
            repoUsers.removeTokenByValidationInfo(tokenValidationInfo)
            true
        }
    }

    fun getUserByToken(token: String): User? {
        if (!canBeToken(token)) {
            return null
        }

        return trxManager.run {
            val tokenValidationInfo = tokenEncoder.createValidationInformation(token)
            val userAndToken = repoUsers.getTokenByTokenValidationInfo(tokenValidationInfo)

            if (userAndToken != null && isTokenTimeValid(clock, userAndToken.second)) {
                repoUsers.updateTokenLastUsed(userAndToken.second, clock.instant())
                userAndToken.first
            } else {
                null
            }
        }
    }

    fun createAppInvite(userId: Int): Either<AuthTokenError, String> {
        return trxManager.run {
            val newInvite = inviteDomain.generateInviteValue()
            val inviteValidationInfo = inviteDomain.createInviteValidationInformation(newInvite)
            val state = inviteDomain.validState
            val now = clock.instant()
            repoInvite.createAppInvite(userId, inviteValidationInfo, state, now)
                ?: return@run failure(AuthTokenError.UserNotFoundOrInvalidCredentials)
            success(newInvite)
        }
    }

    private fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ) = passwordEncoder.matches(password, validationInfo.validationInfo)

    private fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(validationInfo = passwordEncoder.encode(password))

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
