package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.domain.games.utils.Face
import pt.isel.domain.games.utils.State
import java.sql.ResultSet

class JdbiGamesRepository(
    private val handle: Handle,
) : RepositoryGame {
    override fun findById(id: Int): Game? =
        handle
            .createQuery(
                """
                SELECT g.*, l.*, u.id as host_id, u.username as host_username, u.email as host_email,
                       u.balance as host_balance, u.password_hash as host_password_hash
                FROM dbo.GAME g
                JOIN dbo.LOBBY l ON g.lobby_id = l.id
                JOIN dbo.USERS u ON l.host_id = u.id
                WHERE g.id = :id
                """,
            ).bind("id", id)
            .map { rs, _ -> mapRowToGame(rs) }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Game> =
        handle
            .createQuery(
                """
                SELECT g.*, l.*, u.id as host_id, u.username as host_username, u.email as host_email,
                       u.balance as host_balance, u.password_hash as host_password_hash
                FROM dbo.GAME g
                JOIN dbo.LOBBY l ON g.lobby_id = l.id
                JOIN dbo.USERS u ON l.host_id = u.id
                """,
            ).map { rs, _ -> mapRowToGame(rs) }
            .list()

    override fun save(entity: Game) {
        handle
            .createUpdate(
                """
                UPDATE dbo.GAME 
                SET state = :state::dbo.GAME_STATE, current_round = :current_round, 
                    total_rounds = :total_rounds, ended_at = :ended_at
                WHERE id = :id
                """,
            ).bind("id", entity.gid)
            .bind("state", entity.state.name)
            .bind("current_round", entity.currentRound?.number ?: 0)
            .bind("total_rounds", entity.numberOfRounds)
            .bind("ended_at", entity.endedAt)
            .execute()

        // Update round data if current round exists
        entity.currentRound?.let { round ->
            saveRound(entity.gid, round)
        }
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.GAME WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.GAME").execute()
    }

    override fun createGame(
        startedAt: Long,
        lobby: Lobby,
        numberOfRounds: Int,
    ): Game {
        val id =
            handle
                .createUpdate(
                    """
                    INSERT INTO dbo.GAME (lobby_id, state, current_round, total_rounds, created_at) 
                    VALUES (:lobby_id, :state::dbo.GAME_STATE, :current_round, :total_rounds, :created_at)
                    """,
                ).bind("lobby_id", lobby.id)
                .bind("state", State.WAITING.name)
                .bind("current_round", 0)
                .bind("total_rounds", numberOfRounds)
                .bind("created_at", startedAt)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        // Add all lobby players to game
        lobby.users.forEach { user ->
            handle
                .createUpdate(
                    """
                    INSERT INTO dbo.GAME_PLAYER (game_id, user_id, score)
                    VALUES (:game_id, :user_id, :score)
                    """,
                ).bind("game_id", id)
                .bind("user_id", user.id)
                .bind("score", 0)
                .execute()
        }

        return Game(id, startedAt, null, lobby, numberOfRounds, State.WAITING, null)
    }

    override fun endGame(
        game: Game,
        endedAt: Long,
    ): Game {
        handle
            .createUpdate(
                """
                UPDATE dbo.GAME 
                SET state = :state::dbo.GAME_STATE, ended_at = :ended_at
                WHERE id = :id
                """,
            ).bind("id", game.gid)
            .bind("state", State.FINISHED.name)
            .bind("ended_at", endedAt)
            .execute()

        return game.copy(state = State.FINISHED, endedAt = endedAt)
    }

    private fun saveRound(
        gameId: Int,
        round: Round,
    ) {
        // Upsert round
        handle
            .createUpdate(
                """
                INSERT INTO dbo.ROUND (game_id, round_number, turn_of_player, pot)
                VALUES (:game_id, :round_number, :turn_of_player, :pot)
                ON CONFLICT (game_id, round_number) 
                DO UPDATE SET turn_of_player = :turn_of_player, pot = :pot
                """,
            ).bind("game_id", gameId)
            .bind("round_number", round.number)
            .bind("turn_of_player", round.turn.user.id)
            .bind("pot", round.ante)
            .execute()

        // Save player hands
        round.userHands.forEach { (user, hand) ->
            val diceValues = hand.dices.map { faceToChar(it.face) }.toTypedArray()
            handle
                .createUpdate(
                    """
                    INSERT INTO dbo.PLAYER_HAND (game_id, round_number, user_id, dice_values, rolls_left)
                    VALUES (:game_id, :round_number, :user_id, :dice_values, :rolls_left)
                    ON CONFLICT (game_id, round_number, user_id)
                    DO UPDATE SET dice_values = :dice_values, rolls_left = :rolls_left
                    """,
                ).bind("game_id", gameId)
                .bind("round_number", round.number)
                .bind("user_id", user.id)
                .bind("dice_values", diceValues)
                .bind("rolls_left", 0)
                .execute()
        }
    }

    private fun mapRowToGame(rs: ResultSet): Game {
        val gameId = rs.getInt("id")
        val lobbyId = rs.getInt("lobby_id")

        val host =
            User(
                rs.getInt("host_id"),
                rs.getString("host_username"),
                rs.getString("host_email"),
                rs.getInt("host_balance"),
                PasswordValidationInfo(rs.getString("host_password_hash")),
            )

        // Fetch lobby players
        val lobbyPlayers =
            handle
                .createQuery(
                    """
                    SELECT u.* FROM dbo.USERS u
                    JOIN dbo.LOBBY_PLAYER lp ON u.id = lp.user_id
                    WHERE lp.lobby_id = :lobby_id
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

        val lobby =
            Lobby(
                id = lobbyId,
                name = rs.getString("name"),
                description = rs.getString("description"),
                minPlayers = rs.getInt("min_players"),
                maxPlayers = rs.getInt("max_players"),
                users = lobbyPlayers,
                host = host,
            )

        val currentRoundNumber = rs.getInt("current_round")
        val currentRound = if (currentRoundNumber > 0) loadRound(gameId, currentRoundNumber, lobbyPlayers) else null

        return Game(
            gid = gameId,
            startedAt = rs.getLong("created_at"),
            endedAt = rs.getLong("ended_at").takeIf { !rs.wasNull() },
            lobby = lobby,
            numberOfRounds = rs.getInt("total_rounds"),
            state = State.valueOf(rs.getString("state")),
            currentRound = currentRound,
        )
    }

    private fun loadRound(
        gameId: Int,
        roundNumber: Int,
        players: List<User>,
    ): Round? {
        val roundData =
            handle
                .createQuery(
                    """
                    SELECT * FROM dbo.ROUND
                    WHERE game_id = :game_id AND round_number = :round_number
                    """,
                ).bind("game_id", gameId)
                .bind("round_number", roundNumber)
                .mapToMap()
                .findOne()
                .orElse(null) ?: return null

        val turnUserId = roundData["turn_of_player"] as Int
        val turnUser = players.first { it.id == turnUserId }
        val pot = roundData["pot"] as Int

        // Load player hands
        val userHands =
            handle
                .createQuery(
                    """
                    SELECT * FROM dbo.PLAYER_HAND
                    WHERE game_id = :game_id AND round_number = :round_number
                    """,
                ).bind("game_id", gameId)
                .bind("round_number", roundNumber)
                .mapToMap()
                .list()
                .associate { handData ->
                    val userId = handData["user_id"] as Int
                    val user = players.first { it.id == userId }
                    @Suppress("UNCHECKED_CAST")
                    val diceValues = (handData["dice_values"] as Array<String>).map { charToFace(it[0]) }
                    val hand = Hand(diceValues.map { Dice(it) })
                    user to hand
                }

        return Round(
            number = roundNumber,
            firstPlayerIdx = 1,
            turn = Turn(turnUser, userHands[turnUser] ?: Hand(emptyList())),
            users = players,
            userHands = userHands,
            ante = pot,
        )
    }

    private fun faceToChar(face: Face): Char =
        when (face) {
            Face.ACE -> 'A'
            Face.KING -> 'K'
            Face.QUEEN -> 'Q'
            Face.JACK -> 'J'
            Face.TEN -> 'T'
            Face.NINE -> '9'
        }

    private fun charToFace(char: Char): Face =
        when (char) {
            'A' -> Face.ACE
            'K' -> Face.KING
            'Q' -> Face.QUEEN
            'J' -> Face.JACK
            'T' -> Face.TEN
            '9' -> Face.NINE
            else -> throw IllegalArgumentException("Invalid dice face: $char")
        }
}
