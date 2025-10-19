package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import java.sql.ResultSet

class JdbiLobbiesRepository(
    private val handle: Handle,
) : RepositoryLobby {
    override fun findById(id: Int): Lobby? =
        handle
            .createQuery(
                """
                SELECT l.*, u.id as host_id, u.username as host_username, u.email as host_email,
                       u.balance as host_balance, u.password_hash as host_password_hash
                FROM dbo.LOBBY l
                JOIN dbo.USERS u ON l.host_id = u.id
                WHERE l.id = :id
                """,
            ).bind("id", id)
            .map { rs, _ -> mapRowToLobby(rs) }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Lobby> =
        handle
            .createQuery(
                """
                SELECT l.*, u.id as host_id, u.username as host_username, u.email as host_email,
                       u.balance as host_balance, u.password_hash as host_password_hash
                FROM dbo.LOBBY l
                JOIN dbo.USERS u ON l.host_id = u.id
                """,
            ).map { rs, _ -> mapRowToLobby(rs) }
            .list()

    override fun save(entity: Lobby) {
        handle
            .createUpdate(
                """
                UPDATE dbo.LOBBY
                SET name = :name, description = :description, host_id = :host_id,
                    min_players = :min_players, max_players = :max_players
                WHERE id = :id
                """,
            ).bind("id", entity.id)
            .bind("name", entity.name)
            .bind("description", entity.description)
            .bind("host_id", entity.host.id)
            .bind("min_players", entity.minPlayers)
            .bind("max_players", entity.maxPlayers)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.LOBBY WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.LOBBY").execute()
    }

    override fun createLobby(
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        host: User,
    ): Lobby {
        val id =
            handle
                .createUpdate(
                    """
                    INSERT INTO dbo.LOBBY (name, description, host_id, min_players, max_players)
                    VALUES (:name, :description, :host_id, :min_players, :max_players)
                    """,
                ).bind("name", name)
                .bind("description", description)
                .bind("host_id", host.id)
                .bind("min_players", minPlayers)
                .bind("max_players", maxPlayers)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        // Add host as first player in lobby
        handle
            .createUpdate(
                """
                INSERT INTO dbo.LOBBY_USER (lobby_id, user_id)
                VALUES (:lobby_id, :user_id)
                """,
            ).bind("lobby_id", id)
            .bind("user_id", host.id)
            .execute()

        return Lobby(id, name, description, minPlayers, maxPlayers, listOf(host), host)
    }

    override fun findByName(name: String): Lobby? =
        handle
            .createQuery(
                """
                SELECT l.*, u.id as host_id, u.username as host_username, u.email as host_email,
                       u.balance as host_balance, u.password_hash as host_password_hash
                FROM dbo.LOBBY l
                JOIN dbo.USERS u ON l.host_id = u.id
                WHERE l.name = :name
                """,
            ).bind("name", name)
            .map { rs, _ -> mapRowToLobby(rs) }
            .findOne()
            .orElse(null)

    override fun deleteLobbyByHost(host: User) {
        handle
            .createUpdate("DELETE FROM dbo.LOBBY WHERE host_id = :host_id")
            .bind("host_id", host.id)
            .execute()
    }

    override fun deleteLobbyById(id: Int) {
        deleteById(id)
    }

    private fun mapRowToLobby(rs: ResultSet): Lobby {
        val lobbyId = rs.getInt("id")
        val host =
            User(
                rs.getInt("host_id"),
                rs.getString("host_username"),
                rs.getString("host_email"),
                rs.getInt("host_balance"),
                PasswordValidationInfo(rs.getString("host_password_hash")),
            )

        // Fetch all players in this lobby
        val players =
            handle
                .createQuery(
                    """
                    SELECT u.* FROM dbo.USERS u
                    JOIN dbo.LOBBY_USER lu ON u.id = lu.user_id
                    WHERE lu.lobby_id = :lobby_id
                    """,
                ).bind("lobby_id", lobbyId)
                .map { playerRs, _ ->
                    User(
                        playerRs.getInt("id"),
                        playerRs.getString("username"),
                        playerRs.getString("email"),
                        playerRs.getInt("balance"),
                        PasswordValidationInfo(playerRs.getString("password_hash")),
                    )
                }.list()

        return Lobby(
            id = lobbyId,
            name = rs.getString("name"),
            description = rs.getString("description"),
            minPlayers = rs.getInt("min_players"),
            maxPlayers = rs.getInt("max_players"),
            users = players,
            host = host,
        )
    }
}
