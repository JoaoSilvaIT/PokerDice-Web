package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.lobby.LobbyExternalInfo
import pt.isel.domain.lobby.LobbySettings
import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo
import java.sql.ResultSet

class JdbiLobbiesRepository(
    private val handle: Handle,
) : RepositoryLobby {
    override fun findById(id: Int): Lobby? =
        handle
            .createQuery(
                """
                SELECT l.*, u.id as host_id, u.username as host_username, u.balance as host_balance
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
                SELECT l.*, u.id as host_id, u.username as host_username, u.balance as host_balance
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
            .bind("min_players", entity.settings.minPlayers)
            .bind("max_players", entity.settings.maxPlayers)
            .execute()

        // Update players in lobby
        handle
            .createUpdate("DELETE FROM dbo.LOBBY_USER WHERE lobby_id = :lobby_id")
            .bind("lobby_id", entity.id)
            .execute()

        entity.players.forEach { player ->
            handle
                .createUpdate(
                    """
                    INSERT INTO dbo.LOBBY_USER (lobby_id, user_id)
                    VALUES (:lobby_id, :user_id)
                    """,
                ).bind("lobby_id", entity.id)
                .bind("user_id", player.id)
                .execute()
        }
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
                .executeAndReturnGeneratedKeys("id")
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

        val settings = LobbySettings(
            numberOfRounds = 3, // Default value, will be set when game is created
            minPlayers = minPlayers,
            maxPlayers = maxPlayers
        )
        val hostInfo = UserExternalInfo(host.id, host.name, host.balance)
        return Lobby(id, name, description, hostInfo, settings, setOf(hostInfo))
    }

    override fun findByName(name: String): Lobby? =
        handle
            .createQuery(
                """
                SELECT l.*, u.id as host_id, u.username as host_username, u.balance as host_balance
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

    override fun getLobbyById(id: Int): LobbyExternalInfo {
        val lobby = findById(id) ?: throw NoSuchElementException("Lobby with id $id not found")
        return LobbyExternalInfo(
            name = lobby.name,
            description = lobby.description,
            currentPlayers = lobby.players.size,
            numberOfRounds = lobby.settings.numberOfRounds
        )
    }

    private fun mapRowToLobby(rs: ResultSet): Lobby {
        val lobbyId = rs.getInt("id")
        val hostInfo = UserExternalInfo(
            rs.getInt("host_id"),
            rs.getString("host_username"),
            rs.getInt("host_balance")
        )

        // Fetch all players in this lobby
        val players =
            handle
                .createQuery(
                    """
                    SELECT u.id, u.username, u.balance FROM dbo.USERS u
                    JOIN dbo.LOBBY_USER lu ON u.id = lu.user_id
                    WHERE lu.lobby_id = :lobby_id
                    """,
                ).bind("lobby_id", lobbyId)
                .map { playerRs, _ ->
                    UserExternalInfo(
                        playerRs.getInt("id"),
                        playerRs.getString("username"),
                        playerRs.getInt("balance")
                    )
                }.list().toSet()

        val settings = LobbySettings(
            numberOfRounds = 3, // Default value, actual rounds set when game is created
            minPlayers = rs.getInt("min_players"),
            maxPlayers = rs.getInt("max_players")
        )

        return Lobby(
            id = lobbyId,
            name = rs.getString("name"),
            description = rs.getString("description"),
            host = hostInfo,
            settings = settings,
            players = players
        )
    }
}
