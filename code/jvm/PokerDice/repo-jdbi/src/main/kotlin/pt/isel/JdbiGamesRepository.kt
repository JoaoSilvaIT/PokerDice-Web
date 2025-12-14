package pt.isel

import org.jdbi.v3.core.Handle
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Game
import pt.isel.domain.games.Hand
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.games.utils.State
import pt.isel.domain.games.utils.charToFace
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

        // Find next non-folded player
        for (i in 1..round.players.size) {
            val nextPlayerIndex = (currentPlayerIndex + i) % round.players.size
            val nextPlayer = round.players[nextPlayerIndex]

            // Check if player has folded
            val isFolded = round.foldedPlayers.any { it.id == nextPlayer.id }

            // Check if player has already played this round (has 5 dice saved)
            // This is crucial to detect when the round is complete (all players played)
            val existingTurn = handle.createQuery(
                """
                SELECT dice_values FROM dbo.TURN
                WHERE game_id = :game_id AND round_number = :round_number AND user_id = :user_id
                """
            )
                .bind("game_id", round.gameId)
                .bind("round_number", round.number)
                .bind("user_id", nextPlayer.id)
                .mapToMap()
                .findOne()
                .orElse(null)

            val diceValuesObj = existingTurn?.get("dice_values")
            val diceCount = if (diceValuesObj is java.sql.Array) {
                (diceValuesObj.array as? Array<*>)?.size ?: 0
            } else {
                0
            }

            val hasAlreadyPlayed = diceCount >= 5

            if (!isFolded && !hasAlreadyPlayed) {
                return round.copy(
                    turn = Turn(nextPlayer, rollsRemaining = 3, currentDice = emptyList())
                )
            }
        }

        return round
    }

    override fun distributeWinnings(round: Round): Round {
        val winningsPerWinner = round.pot / round.winners.size

        val updatedPlayers = round.players.map { player ->
            if (round.winners.any { it.id == player.id }) {
                handle
                    .createUpdate(
                        """
                    UPDATE dbo.USERS
                    SET balance = balance + :winnings
                    WHERE id = :user_id
                    """
                    ).bind("winnings", winningsPerWinner)
                    .bind("user_id", player.id)
                    .execute()

                player.copy(
                    currentBalance = player.currentBalance + winningsPerWinner,
                    moneyWon = player.moneyWon + winningsPerWinner
                )
            } else {
                player
            }
        }

        // Store the winnings amount in ROUND_WINNER table - use UPSERT to ensure it's always set
        round.winners.forEach { winner ->

            val rowsAffected = handle.createUpdate(
                """
                INSERT INTO dbo.ROUND_WINNER (game_id, round_number, user_id, winnings_amount)
                VALUES (:game_id, :round_number, :user_id, :winnings)
                ON CONFLICT (game_id, round_number, user_id)
                DO UPDATE SET winnings_amount = EXCLUDED.winnings_amount
                """
            )
                .bind("winnings", winningsPerWinner)
                .bind("game_id", round.gameId)
                .bind("round_number", round.number)
                .bind("user_id", winner.id)
                .execute()

            // Verify the value was actually set
            val storedValue = handle.createQuery(
                "SELECT winnings_amount FROM dbo.ROUND_WINNER WHERE game_id = :game_id AND round_number = :round_number AND user_id = :user_id"
            )
                .bind("game_id", round.gameId)
                .bind("round_number", round.number)
                .bind("user_id", winner.id)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(-1)

        }


        return round.copy(players = updatedPlayers, pot = 0)
    }


    private fun saveRound(gameId: Int, round: Round) {
        handle.createUpdate(
            """
        INSERT INTO dbo.ROUND (game_id, round_number, first_player_idx, turn_of_player, ante, pot)
        VALUES (:game_id, :round_number, :first_player_idx, :turn_of_player, :ante, :pot)
        ON CONFLICT (game_id, round_number)
        DO UPDATE SET
            first_player_idx = EXCLUDED.first_player_idx,
            turn_of_player = EXCLUDED.turn_of_player,
            ante = EXCLUDED.ante,
            pot = EXCLUDED.pot
        """
        )
            .bind("game_id", gameId)
            .bind("round_number", round.number)
            .bind("first_player_idx", round.firstPlayerIdx)
            .bind("turn_of_player", round.turn.player.id)
            .bind("ante", round.ante)
            .bind("pot", round.pot)
            .execute()

        val diceArray = round.turn.currentDice.map { faceToChar(it.face).toString() }.toTypedArray()

        handle.createUpdate(
            """
        INSERT INTO dbo.TURN (game_id, round_number, user_id, dice_values, rolls_left)
        VALUES (:game_id, :round_number, :user_id, :dice_values, :rolls_left)
        ON CONFLICT (game_id, round_number, user_id)
        DO UPDATE SET dice_values = :dice_values, rolls_left = :rolls_left
        WHERE dbo.TURN.rolls_left <> -1
        """
        )
            .bind("game_id", gameId)
            .bind("round_number", round.number)
            .bind("user_id", round.turn.player.id)
            .bindArray("dice_values", String::class.java, *diceArray)
            .bind("rolls_left", round.turn.rollsRemaining)
            .execute()

        // Only insert winners if they don't already exist (to avoid overwriting winnings_amount)
        if (round.winners.isNotEmpty()) {
            round.winners.forEach { winner ->
                handle.createUpdate(
                    """
                INSERT INTO dbo.ROUND_WINNER (game_id, round_number, user_id, winnings_amount)
                VALUES (:game_id, :round_number, :user_id, 0)
                ON CONFLICT (game_id, round_number, user_id) DO NOTHING
                """
                )
                    .bind("game_id", gameId)
                    .bind("round_number", round.number)
                    .bind("user_id", winner.id)
                    .execute()
            }
        }
    }



    override fun updateTurn(chosenDice: Dice, round: Round): Round {
        val existingDice = handle.createQuery(
            """
        SELECT dice_values FROM dbo.TURN
        WHERE game_id = :game_id AND round_number = :round_number AND user_id = :user_id
        """
        )
            .bind("game_id", round.gameId)
            .bind("round_number", round.number)
            .bind("user_id", round.turn.player.id)
            .mapTo(String::class.java)
            .findOne()
            .map { it.removeSurrounding("{", "}").split(",").toTypedArray() }  // Parse manual do array
            .orElse(emptyArray())

        val updatedDiceArray = existingDice + faceToChar(chosenDice.face).toString()
        val updatedDice = round.turn.currentDice + chosenDice
        val updatedTurn = round.turn.copy(currentDice = updatedDice)

        handle.createUpdate(
            """
        INSERT INTO dbo.TURN (game_id, round_number, user_id, dice_values, rolls_left)
        VALUES (:game_id, :round_number, :user_id, :dice_values, :rolls_left)
        ON CONFLICT (game_id, round_number, user_id)
        DO UPDATE SET dice_values = :dice_values, rolls_left = :rolls_left
        WHERE dbo.TURN.rolls_left <> -1
        """
        )
            .bind("game_id", round.gameId)
            .bind("round_number", round.number)
            .bind("user_id", updatedTurn.player.id)
            .bindArray("dice_values", String::class.java, *updatedDiceArray)
            .bind("rolls_left", updatedTurn.rollsRemaining)
            .execute()

        return round.copy(turn = updatedTurn)
    }

    override fun loadPlayerHands(gameId: Int, roundNumber: Int, players: List<PlayerInGame>): Map<PlayerInGame, Hand> {
        return handle.createQuery(
            """
        SELECT user_id, dice_values FROM dbo.TURN
        WHERE game_id = :game_id AND round_number = :round_number
        """
        )
            .bind("game_id", gameId)
            .bind("round_number", roundNumber)
            .mapToMap()
            .list()
            .mapNotNull { row ->
                val userId = row["user_id"] as Int
                val player = players.firstOrNull { it.id == userId } ?: return@mapNotNull null

                val diceArray = (row["dice_values"] as java.sql.Array).array as Array<*>
                val dices = diceArray.mapNotNull { it?.toString()?.firstOrNull() }
                    .map { Dice(charToFace(it)) }
                player to Hand(dices)
            }
            .toMap()
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
                    val playerId = playerRs.getInt("id")
                    val moneyWon = calculatePlayerMoneyWon(gameId, playerId)
                    PlayerInGame(
                        playerId,
                        playerRs.getString("username"),
                        playerRs.getInt("balance"),
                        moneyWon
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

    private fun calculatePlayerMoneyWon(gameId: Int, userId: Int): Int {
        // Simply sum up the winnings_amount from ROUND_WINNER table
        return handle.createQuery(
            """
            SELECT COALESCE(SUM(winnings_amount), 0) as total
            FROM dbo.ROUND_WINNER
            WHERE game_id = :game_id AND user_id = :user_id
            """
        )
            .bind("game_id", gameId)
            .bind("user_id", userId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)
    }

    private fun loadRound(gameId: Int, roundNumber: Int, players: List<PlayerInGame>): Round? {
        val roundData = handle.createQuery(
            """
        SELECT * FROM dbo.ROUND
        WHERE game_id = :game_id AND round_number = :round_number
        """
        )
            .bind("game_id", gameId)
            .bind("round_number", roundNumber)
            .mapToMap()
            .findOne()
            .orElse(null) ?: return null

        val turnUserId = roundData["turn_of_player"] as Int
        val turnPlayer = players.first { it.id == turnUserId }

        val turnData = handle.createQuery(
            """
        SELECT dice_values, rolls_left FROM dbo.TURN
        WHERE game_id = :game_id AND round_number = :round_number AND user_id = :user_id
        """
        )
            .bind("game_id", gameId)
            .bind("round_number", roundNumber)
            .bind("user_id", turnUserId)
            .mapToMap()
            .findOne()
            .orElse(null)

        val currentDice = turnData?.get("dice_values")?.toString()
            ?.removeSurrounding("{", "}")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { Dice(charToFace(it[0])) } ?: emptyList()

        val rollsLeft = turnData?.get("rolls_left") as? Int ?: 3

        // Carregar winners
        val winnerIds = handle.createQuery(
            """
        SELECT user_id FROM dbo.ROUND_WINNER
        WHERE game_id = :game_id AND round_number = :round_number
        """
        )
            .bind("game_id", gameId)
            .bind("round_number", roundNumber)
            .mapTo(Int::class.java)
            .list()

        val winners = players.filter { it.id in winnerIds }

        // Load folded players
        val foldedPlayerIds = handle.createQuery(
            """
            SELECT user_id FROM dbo.TURN
            WHERE game_id = :game_id AND round_number = :round_number AND rolls_left = -1
            """
        )
            .bind("game_id", gameId)
            .bind("round_number", roundNumber)
            .mapTo(Int::class.java)
            .list()

        val foldedPlayers = players.filter { it.id in foldedPlayerIds }

        return Round(
            number = roundNumber,
            firstPlayerIdx = roundData["first_player_idx"] as Int,
            turn = Turn(turnPlayer, rollsRemaining = rollsLeft, currentDice = currentDice),
            players = players,
            playerHands = emptyMap(),
            gameId = gameId,
            ante = roundData["ante"] as Int,
            pot = roundData["pot"] as Int,
            winners = winners,
            foldedPlayers = foldedPlayers
        )
    }


    override fun findActiveGamesByLobbyId(lobbyId: Int): List<Game> =
        handle.createQuery(
            """
        SELECT g.*
        FROM dbo.GAME g
        WHERE g.lobby_id = :lobby_id 
          AND g.state IN ('WAITING', 'RUNNING')
        """
        )
            .bind("lobby_id", lobbyId)
            .map { rs, _ -> mapRowToGame(rs) }
            .list()
}