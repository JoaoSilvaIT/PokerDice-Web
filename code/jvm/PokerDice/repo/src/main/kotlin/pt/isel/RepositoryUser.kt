package pt.isel

import org.springframework.stereotype.Component
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.Token
import pt.isel.domain.users.TokenValidationInfo
import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo
import java.time.Instant

/**
 * Repository interface for managing users, extends the generic Repository
 */
@Component
interface RepositoryUser : Repository<User> {
    fun createUser(
        name: String,
        email: String,
        passwordValidation: PasswordValidationInfo,
    ): User

    fun findByEmail(email: String): User?

    fun getUserById(id: Int): UserExternalInfo

    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    )

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int
}
