package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.Token
import pt.isel.domain.users.TokenValidationInfo
import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo
import pt.isel.domain.users.UserStatistics
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Hand
import pt.isel.domain.games.utils.charToFace
import pt.isel.domain.games.utils.defineHandRank
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
                SELECT u.id, u.username, u.email, u.balance, u.password_hash,
                       t.token, t.user_id, t.created_at, t.last_used_at
                FROM dbo.TOKEN t
                JOIN dbo.USERS u ON t.user_id = u.id
                WHERE t.token = :token
                """,
            ).bind("token", tokenValidationInfo.validationInfo)
            .map { rs, _ ->
                val user = User(
                    id = rs.getInt("id"),
                    name = rs.getString("username"),
                    email = rs.getString("email"),
                    balance = rs.getInt("balance"),
                    passwordValidation = PasswordValidationInfo(rs.getString("password_hash")),
                )
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

    override fun getUserById(id: Int): UserExternalInfo? {
        return handle
            .createQuery(
                """
            SELECT id, username, balance
            FROM dbo.USERS
            WHERE id = :id
            """,
            ).bind("id", id)
            .map { rs, _ ->
                UserExternalInfo(
                    id = rs.getInt("id"),
                    name = rs.getString("username"),
                    balance = rs.getInt("balance")
                )
            }
            .findOne()
            .orElse(null)
    }

    override fun getUserStats(userId: Int): UserStatistics {
        val gamesPlayed = handle.createQuery(
            """
            SELECT COUNT(DISTINCT game_id) 
            FROM dbo.TURN 
            WHERE user_id = :user_id
            """
        )
            .bind("user_id", userId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)

        val wins = handle.createQuery(
            """
            WITH GameTotals AS (
                SELECT game_id, user_id, SUM(winnings_amount) as total_won
                FROM dbo.ROUND_WINNER
                GROUP BY game_id, user_id
            ),
            GameWinners AS (
                SELECT game_id, user_id
                FROM GameTotals gt
                WHERE total_won = (SELECT MAX(total_won) FROM GameTotals WHERE game_id = gt.game_id)
            )
            SELECT COUNT(*) 
            FROM GameWinners 
            WHERE user_id = :user_id
            """
        )
            .bind("user_id", userId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)

        val losses = if (gamesPlayed > wins) gamesPlayed - wins else 0
        val winRate = if (gamesPlayed > 0) wins.toDouble() / gamesPlayed else 0.0

        val handFrequencies = handle.createQuery(
            """
            SELECT dice_values 
            FROM dbo.TURN 
            WHERE user_id = :user_id
            """
        ).bind("user_id", userId)
            .map { rs, _ ->
                val sqlArray = rs.getArray("dice_values")
                val strArray = sqlArray.array as Array<String>
                val dices = strArray.map { charStr ->
                    val char = charStr.first()
                    Dice(charToFace(char))
                }
                val hand = Hand(dices)
                defineHandRank(hand).second
            }
            .list()
            .groupingBy { it }
            .eachCount()

        return UserStatistics(gamesPlayed, wins, losses, winRate, handFrequencies)
    }

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

    override fun addBalance(userId: Int, amount: Int) {
        handle.createUpdate("UPDATE dbo.USERS SET balance = balance + :amount WHERE id = :id")
            .bind("amount", amount)
            .bind("id", userId)
            .execute()
    }

    private fun mapRowToUser(rs: ResultSet): User =
        User(
            id = rs.getInt("id"),
            name = rs.getString("username"),
            email = rs.getString("email"),
            balance = rs.getInt("balance"),
            passwordValidation = PasswordValidationInfo(rs.getString("password_hash")),
        )
}