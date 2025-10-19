package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.games.utils.Face
import pt.isel.domain.games.utils.State
import pt.isel.domain.games.utils.faceToChar
import java.sql.ResultSet

class JdbiGamesRepository(
    private val handle: Handle,
) : RepositoryGame {
    override fun findById(id: Int): Game? =
        handle
            .createQuery(
                """
                SELECT g.*
                FROM dbo.GAME g
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
                SELECT g.*
                FROM dbo.GAME g
                """,
            ).map { rs, _ -> mapRowToGame(rs) }
            .list()

    override fun save(entity: Game) {
        handle
            .createUpdate(
                """
                UPDATE dbo.GAME
                SET state = :state::dbo.GAME_STATE, current_round_number = :current_round_number,
                    total_rounds = :total_rounds, ended_at = :ended_at
                WHERE id = :id
                """,
            ).bind("id", entity.id)
            .bind("state", entity.state.name)
            .bind("current_round_number", entity.currentRound?.number ?: 0)
            .bind("total_rounds", entity.numberOfRounds)
            .bind("ended_at", entity.endedAt)
            .execute()

        // Update round data if current round exists
        entity.currentRound?.let { round ->
            saveRound(entity.id, round)
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
                INSERT INTO dbo.GAME (lobby_id, state, current_round_number, total_rounds, started_at)
                VALUES (:lobby_id, :state::dbo.GAME_STATE, :current_round_number, :total_rounds, :started_at)
                """,
                ).bind("lobby_id", lobby.id)
                .bind("state", State.WAITING.name)
                .bind("current_round_number", 0)
                .bind("total_rounds", numberOfRounds)
                .bind("started_at", startedAt)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Int::class.java)
                .one()
        val players = lobby.players.map { PlayerInGame(it.id, it.name, 0, 0) }.toList()
        return Game(
            id = id,
            lobbyId = lobby.id,
            players = players,
            numberOfRounds = numberOfRounds,
            state = State.WAITING,
            currentRound = null,
            startedAt = startedAt,
            endedAt = null
        )
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
            ).bind("id", game.id)
            .bind("state", State.FINISHED.name)
            .bind("ended_at", endedAt)
            .execute()

        return game.copy(state = State.FINISHED, endedAt = endedAt)
    }

    override fun updateGameRound(round: Round, game: Game): Game {
        saveRound(game.id, round)
        val updatedGame = game.copy(currentRound = round)
        save(updatedGame)
        return updatedGame
    }

    override fun startNewRound(game: Game): Game {
        val nextRoundNumber = (game.currentRound?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNumber - 1) % game.players.size
        val firstPlayer = game.players[firstPlayerIndex]

        val newRound = Round(
            number = nextRoundNumber,
            firstPlayerIdx = firstPlayerIndex,
            turn = Turn(firstPlayer, rollsRemaining = 3, currentDice = emptyList()),
            players = game.players,
            playerHands = emptyMap(),
            gameId = game.id
        )

        val updatedGame = game.copy(currentRound = newRound)
        save(updatedGame)
        return updatedGame
    }

    override fun setAnte(ante: Int, round: Round): Round {
        val updatedRound = round.copy(ante = ante)
        return updatedRound
    }

    override fun payAnte(round: Round): Round {
        round.players.forEach { player ->
            handle
                .createUpdate(
                    """
                    UPDATE dbo.USERS
                    SET balance = balance - :ante
                    WHERE id = :user_id
                    """,
                ).bind("ante", round.ante)
                .bind("user_id", player.id)
                .execute()
        }

        val updatedPlayers = round.players.map { player ->
            player.copy(currentBalance = player.currentBalance - round.ante)
        }

        val newPot = round.pot + (round.ante * round.players.size)
        val updatedRound = round.copy(
            players = updatedPlayers,
            pot = newPot
        )

        return updatedRound
    }

    override fun nextTurn(round: Round): Round {
        val currentPlayerIndex = round.players.indexOfFirst { it.id == round.turn.player.id }
        val nextPlayerIndex = (currentPlayerIndex + 1) % round.players.size
        val nextPlayer = round.players[nextPlayerIndex]

        val updatedRound = round.copy(
            turn = Turn(nextPlayer, rollsRemaining = 3, currentDice = emptyList())
        )
        saveRound(updatedRound.gameId, updatedRound)
        return updatedRound
    }

    private fun saveRound(
        gameId: Int,
        round: Round,
    ) {
        // Upsert round
        handle
            .createUpdate(
                """
                INSERT INTO dbo.ROUND (game_id, round_number, first_player_idx, turn_of_player, ante, pot)
                VALUES (:game_id, :round_number, :first_player_idx, :turn_of_player, :ante, :pot)
                ON CONFLICT (game_id, round_number)
                DO UPDATE SET 
                    first_player_idx = EXCLUDED.first_player_idx, 
                    turn_of_player = EXCLUDED.turn_of_player, 
                    ante = EXCLUDED.ante, 
                    pot = EXCLUDED.pot
                """,
            ).bind("game_id", gameId)
            .bind("round_number", round.number)
            .bind("first_player_idx", round.firstPlayerIdx)
            .bind("turn_of_player", round.turn.player.id)
            .bind("ante", round.ante)
            .bind("pot", round.pot)
            .execute()

        // Save player hands
        round.playerHands.forEach { (player, hand) ->
            handle
                .createUpdate(
                    """
                    INSERT INTO dbo.TURN (game_id, round_number, user_id, dice_values, rolls_left)
                    VALUES (:game_id, :round_number, :user_id, :dice_values, :rolls_left)
                    ON CONFLICT (game_id, round_number, user_id)
                    DO UPDATE SET dice_values = :dice_values, rolls_left = :rolls_left
                    """,
                ).bind("game_id", gameId)
                .bind("round_number", round.number)
                .bind("user_id", player.id)
                .bind("dice_values", hand)
                .bind("rolls_left", 0)
                .execute()
        }
    }

    override fun updateTurn(chosenDice: Dice, round: Round): Round {

        val updatedDice = round.turn.currentDice + chosenDice
        val updatedTurn = round.turn.copy(currentDice = updatedDice)
        val dicesToChars = updatedDice.map { faceToChar(it.face).toString() }.toTypedArray()

        handle
            .createUpdate(
            """
        INSERT INTO dbo.TURN (game_id, round_number, user_id, dice_values, rolls_left)
        VALUES (:game_id, :round_number, :user_id, :dice_values, :rolls_left)
        ON CONFLICT (game_id, round_number, user_id)
        DO UPDATE SET dice_values = :dice_values, rolls_left = :rolls_left
        """
        )
            .bind("game_id", round.gameId)
            .bind("round_number", round.number)
            .bind("user_id", updatedTurn.player.id)
            .bind("dice_values",dicesToChars)
            .bind("rolls_left", updatedTurn.rollsRemaining)
            .execute()


        return round.copy(turn = updatedTurn)
    }


    private fun mapRowToGame(rs: ResultSet): Game {
        val gameId = rs.getInt("id")
        val lobbyId = rs.getInt("lobby_id")

        val lobbyPlayers =
            handle
                .createQuery(
                    """
                    SELECT u.id, u.username, u.balance FROM dbo.USERS u
                    JOIN dbo.LOBBY_USER lu ON u.id = lu.user_id
                    WHERE lu.lobby_id = :lobby_id
                    """,
                ).bind("lobby_id", lobbyId)
                .map { playerRs, _ ->
                    PlayerInGame(
                        playerRs.getInt("id"),
                        playerRs.getString("username"),
                        playerRs.getInt("balance"),
                        0 // wins count
                    )
                }.list()

        val currentRoundNumber = rs.getInt("current_round_number")
        val currentRound = if (currentRoundNumber > 0) loadRound(gameId, currentRoundNumber, lobbyPlayers) else null

        return Game(
            id = gameId,
            lobbyId = lobbyId,
            players = lobbyPlayers,
            numberOfRounds = rs.getInt("total_rounds"),
            state = State.valueOf(rs.getString("state")),
            currentRound = currentRound,
            startedAt = rs.getLong("started_at"),
            endedAt = rs.getLong("ended_at").takeIf { !rs.wasNull() }
        )
    }

    private fun loadRound(
        gameId: Int,
        roundNumber: Int,
        players: List<PlayerInGame>,
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
        val turnPlayer = players.first { it.id == turnUserId }
        val pot = roundData["pot"] as Int
        val ante = roundData["ante"] as Int
        val firstPlayerIdx = roundData["first_player_idx"] as Int

        val playerHands = emptyMap<PlayerInGame, Hand>()

        return Round(
            number = roundNumber,
            firstPlayerIdx = firstPlayerIdx,
            turn = Turn(turnPlayer, rollsRemaining = 3, currentDice = emptyList()),
            players = players,
            playerHands = playerHands,
            gameId = gameId,
            ante = ante,
            pot = pot
        )
    }

}
