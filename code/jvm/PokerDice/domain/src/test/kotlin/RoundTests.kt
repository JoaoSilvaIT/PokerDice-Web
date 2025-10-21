import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Hand
import pt.isel.domain.games.MIN_ANTE
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn

class RoundTests {
    private val player1 = PlayerInGame(1, "Alice", 100, 0)
    private val player2 = PlayerInGame(2, "Bob", 100, 0)
    private val players = listOf(player1, player2)

    @Test
    fun `test Round creation with default ante`() {
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = emptyMap(),
                gameId = 1,
            )

        assertEquals(1, round.number)
        assertEquals(player1, round.turn.player)
        assertEquals(2, round.players.size)
        assertEquals(MIN_ANTE, round.ante)
    }

    @Test
    fun `test Round creation with custom ante`() {
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = emptyMap(),
                ante = 20,
                gameId = 1,
            )

        assertEquals(20, round.ante)
    }

    @Test
    fun `test Round with player hands`() {
        val hand1 = Hand(emptyList())
        val hand2 = Hand(emptyList())
        val playerHands = mapOf(player1 to hand1, player2 to hand2)

        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = playerHands,
                gameId = 1,
            )

        assertEquals(2, round.playerHands.size)
        assertEquals(hand1, round.playerHands[player1])
        assertEquals(hand2, round.playerHands[player2])
    }

    @Test
    fun `test Round with pot`() {
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = emptyMap(),
                pot = 100,
                gameId = 1,
            )

        assertEquals(100, round.pot)
    }

    @Test
    fun `test Round with winners`() {
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = emptyMap(),
                winners = listOf(player1),
                gameId = 1,
            )

        assertEquals(1, round.winners.size)
        assertEquals(player1, round.winners[0])
    }

    @Test
    fun `test Round equality`() {
        val round1 =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = emptyMap(),
                gameId = 1,
            )

        val round2 =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = players,
                playerHands = emptyMap(),
                gameId = 1,
            )

        assertEquals(round1, round2)
    }
}
