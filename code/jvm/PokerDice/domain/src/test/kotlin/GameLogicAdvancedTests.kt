import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Hand
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.games.utils.Face
import pt.isel.domain.games.utils.HandRank
import pt.isel.domain.games.utils.calculateFullHandValue
import pt.isel.domain.games.utils.decideRoundWinner
import pt.isel.domain.games.utils.defineHandRank
import pt.isel.domain.games.utils.defineHandRank

class GameLogicAdvancedTests {

    @Test
    fun `test calculateFullHandValue with five of a kind`() {
        val hand = Hand(List(5) { Dice(Face.ACE) })
        val handRank = defineHandRank(hand)
        val value = calculateFullHandValue(handRank)

        // FIVE_OF_A_KIND (8) + ACE (6) = 14
        assertEquals(14, value)
    }

    @Test
    fun `test calculateFullHandValue with different hand ranks`() {
        // Four of a kind with ACE
        val fourAces =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                ),
            )
        val fourAcesRank = defineHandRank(fourAces)
        val fourAcesValue = calculateFullHandValue(fourAcesRank)
        assertEquals(13, fourAcesValue) // 7 + 6

        // Full house with KING as highest
        val fullHouse =
            Hand(
                listOf(
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                    Dice(Face.QUEEN),
                ),
            )
        val fullHouseRank = defineHandRank(fullHouse)
        val fullHouseValue = calculateFullHandValue(fullHouseRank)
        assertEquals(11, fullHouseValue) // 6 + 5
    }

    @Test
    fun `test decideRoundWinner with single winner`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)

        val hand1 = Hand(List(5) { Dice(Face.ACE) }) // Five of a kind
        val hand2 =
            Hand(
                listOf(
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                ),
            ) // Four of a kind

        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = listOf(player1, player2),
                playerHands = mapOf(player1 to hand1, player2 to hand2),
                gameId = 1,
            )

        val winners = decideRoundWinner(round)

        assertEquals(1, winners.size)
        assertEquals(player1, winners[0])
    }

    @Test
    fun `test decideRoundWinner with tie`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)

        val hand1 = Hand(List(5) { Dice(Face.ACE) })
        val hand2 = Hand(List(5) { Dice(Face.ACE) })

        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = listOf(player1, player2),
                playerHands = mapOf(player1 to hand1, player2 to hand2),
                gameId = 1,
            )

        val winners = decideRoundWinner(round)

        assertEquals(2, winners.size)
        assertTrue(winners.contains(player1))
        assertTrue(winners.contains(player2))
    }

    @Test
    fun `test decideRoundWinner with multiple players`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)
        val player3 = PlayerInGame(3, "Charlie", 100, 0)

        val hand1 = Hand(List(5) { Dice(Face.KING) }) // Five of a kind KING
        val hand2 = Hand(List(5) { Dice(Face.ACE) }) // Five of a kind ACE (winner)
        val hand3 =
            Hand(
                listOf(
                    Dice(Face.QUEEN),
                    Dice(Face.QUEEN),
                    Dice(Face.QUEEN),
                    Dice(Face.QUEEN),
                    Dice(Face.JACK),
                ),
            ) // Four of a kind

        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = listOf(player1, player2, player3),
                playerHands = mapOf(player1 to hand1, player2 to hand2, player3 to hand3),
                gameId = 1,
            )

        val winners = decideRoundWinner(round)

        assertEquals(1, winners.size)
        assertEquals(player2, winners[0])
    }

    @Test
    fun `test decideRoundWinner with same rank but different high card`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)

        val hand1 = Hand(List(5) { Dice(Face.KING) }) // Five of a kind KING
        val hand2 = Hand(List(5) { Dice(Face.ACE) }) // Five of a kind ACE (winner)

        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(player1, rollsRemaining = 3),
                players = listOf(player1, player2),
                playerHands = mapOf(player1 to hand1, player2 to hand2),
                gameId = 1,
            )

        val winners = decideRoundWinner(round)

        assertEquals(1, winners.size)
        assertEquals(player2, winners[0])
    }
}

