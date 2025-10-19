package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.Token
import pt.isel.domain.users.TokenValidationInfo
import pt.isel.domain.users.User
import java.sql.ResultSet
import java.time.Instant

class JdbiUsersRepository(
    private val handle: Handle,
) : RepositoryUser {
    override fun findById(id: Int): User? =
        handle
            .createQuery(
                """
                SELECT * FROM dbo.USERS
                WHERE id = :id
                """,
            ).bind("id", id)
            .map { rs, _ -> mapRowToUser(rs) }
            .findOne()
            .orElse(null)

    override fun findAll(): List<User> =
        handle
            .createQuery(
                """
                SELECT * FROM dbo.USERS
                """,
            ).map { rs, _ -> mapRowToUser(rs) }
            .list()

    override fun save(entity: User) {
        handle
            .createUpdate(
                """
                UPDATE dbo.USERS 
                SET username = :username, email = :email, password_hash = :password_hash, balance = :balance
                WHERE id = :id
                """,
            ).bind("id", entity.id)
            .bind("username", entity.name)
            .bind("email", entity.email)
            .bind("password_hash", entity.passwordValidation.validationInfo)
            .bind("balance", entity.balance)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.USERS WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.USERS").execute()
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? {
        return handle
            .createQuery(
                """
                SELECT u.id AS id,
                       u.username AS username,
                       u.password_hash AS password_hash,
                       t.token AS token,
                       t.user_id AS user_id,
                       t.created_at AS created_at,
                       t.last_used_at AS last_used_at
                FROM dbo.TOKEN t
                JOIN dbo.USERS u ON t.user_id = u.id
                WHERE t.token = :token
                """,
            ).bind("token", tokenValidationInfo.validationInfo)
            .map { rs, _ ->
                val user = mapRowToUser(rs)
                val token = Token(
                    tokenValidationInfo = TokenValidationInfo(rs.getString("token")),
                    userId = rs.getInt("user_id"),
                    createdAt = Instant.ofEpochSecond(rs.getLong("created_at")),
                    lastUsedAt = Instant.ofEpochSecond(rs.getLong("last_used_at")),
                )
                Pair(user, token)
            }
            .findOne()
            .orElse(null)
    }

    override fun createUser(
        name: String,
        email: String,
        passwordValidation: PasswordValidationInfo,
    ): User {
        val id = handle
            .createUpdate(
                """
                INSERT INTO dbo.USERS (username, email, password_hash, balance)
                VALUES (:username, :email, :password_hash, :balance)
                """,
            ).bind("username", name)
            .bind("email", email)
            .bind("password_hash", passwordValidation.validationInfo)
            .bind("balance", 100)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

        return User(id, name, email, 100, passwordValidation)
    }

    override fun findByEmail(email: String): User? =
        handle
            .createQuery(
                """
                SELECT * FROM dbo.USERS
                WHERE email = :email
                """,
            ).bind("email", email)
            .map { rs, _ -> mapRowToUser(rs) }
            .findOne()
            .orElse(null)

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        // Before inserting the new token, remove old ones if we're at or over the limit
        val existingTokens: List<String> = handle
            .createQuery(
                """
                SELECT token FROM dbo.TOKEN
                WHERE user_id = :user_id
                ORDER BY created_at ASC
                """,
            ).bind("user_id", token.userId)
            .map { rs, _ -> rs.getString("token") }
            .list()

        // We need to delete tokens if adding one more would exceed maxTokens
        val toDeleteCount = existingTokens.size - maxTokens + 1
        if (existingTokens.size >= maxTokens) {
            // Delete the oldest tokens to make room for the new one
            val tokensToDelete = existingTokens.take(toDeleteCount)
            tokensToDelete.forEach { tk ->
                handle
                    .createUpdate(
                        """
                        DELETE FROM dbo.TOKEN WHERE token = :token
                        """,
                    ).bind("token", tk)
                    .execute()
            }
        }

        handle
            .createUpdate(
                """
                INSERT INTO dbo.TOKEN (token, user_id, created_at, last_used_at)
                VALUES (:token, :user_id, :created_at, :last_used_at)
                """,
            ).bind("token", token.tokenValidationInfo.validationInfo)
            .bind("user_id", token.userId)
            .bind("created_at", token.createdAt.epochSecond)
            .bind("last_used_at", token.lastUsedAt.epochSecond)
            .execute()
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        handle
            .createUpdate(
                """
                UPDATE dbo.TOKEN
                SET last_used_at = :last_used_at
                WHERE user_id = :user_id AND token = :token
                """,
            ).bind("last_used_at", now.epochSecond)
            .bind("user_id", token.userId)
            .bind("token", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        handle
            .createUpdate(
                """
                DELETE FROM dbo.TOKEN
                WHERE token = :token
                """,
            ).bind("token", tokenValidationInfo.validationInfo)
            .execute()

    private fun mapRowToUser(rs: ResultSet): User =
        User(
            id = rs.getInt("id"),
            name = rs.getString("username"),
            email = rs.getString("email"),
            balance = rs.getInt("balance"),
            passwordValidation = PasswordValidationInfo(rs.getString("password_hash")),
        )
}