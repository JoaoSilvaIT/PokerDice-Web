import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Hand
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.games.utils.Face
import pt.isel.domain.games.utils.calculateFullHandValue
import pt.isel.domain.games.utils.decideRoundWinner
import pt.isel.domain.games.utils.defineHandRank

class GameLogicAdvancedTests {
    @Test
    fun `test calculateFullHandValue with five of a kind`() {
        val hand = Hand(List(5) { Dice(Face.ACE) })
        val handRank = defineHandRank(hand)
        val value = calculateFullHandValue(handRank)

        // FIVE_OF_A_KIND (8) * 1000 + ACE (6) * 10 + kicker (0) = 8060
        assertEquals(8060, value)
    }

    @Test
    fun `test calculateFullHandValue with different hand ranks`() {
        // Four of a kind with ACE, kicker KING
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
        assertEquals(7065, fourAcesValue) // 7*1000 + 6*10 + 5

        // Full house with KING triplet, QUEEN pair
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
        assertEquals(6054, fullHouseValue) // 6*1000 + 5*10 + 4
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

    @Test
    fun `test pair of Kings beats pair of 9s even with Ace kicker`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)

        // Player 1: Pair of Kings {9,K,J,T,K}
        val hand1 =
            Hand(
                listOf(
                    Dice(Face.NINE),
                    Dice(Face.KING),
                    Dice(Face.JACK),
                    Dice(Face.TEN),
                    Dice(Face.KING),
                ),
            )

        // Player 2: Pair of 9s with Ace kicker {9,9,J,A,K}
        val hand2 =
            Hand(
                listOf(
                    Dice(Face.NINE),
                    Dice(Face.NINE),
                    Dice(Face.JACK),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                ),
            )

        val rank1 = defineHandRank(hand1)
        val rank2 = defineHandRank(hand2)

        val value1 = calculateFullHandValue(rank1)
        val value2 = calculateFullHandValue(rank2)

        // Both should be ONE_PAIR (strength 2)
        // Player 1: 2*1000 + 5*10 (KING) + 3 (JACK kicker) = 2053
        // Player 2: 2*1000 + 1*10 (NINE) + 6 (ACE kicker) = 2016
        assertTrue(value1 > value2, "Pair of Kings ($value1) should beat pair of 9s ($value2)")

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
        assertEquals(player1, winners[0], "Player 1 with pair of Kings should win")
    }

    @Test
    fun `test same pair rank results in tie`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)

        // Player 1: Pair of Kings {K,K,A,J,T}
        val hand1 =
            Hand(
                listOf(
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.ACE),
                    Dice(Face.JACK),
                    Dice(Face.TEN),
                ),
            )

        // Player 2: Pair of Kings {K,9,A,K,Q}
        val hand2 =
            Hand(
                listOf(
                    Dice(Face.KING),
                    Dice(Face.NINE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                ),
            )

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

        // Both have pair of Kings with ACE kicker - should be a tie
        assertEquals(2, winners.size, "Both players with pair of Kings and same kicker should tie")
    }

    @Test
    fun `test same pair but different kicker - higher kicker wins`() {
        val player1 = PlayerInGame(1, "Alice", 100, 0)
        val player2 = PlayerInGame(2, "Bob", 100, 0)

        // Player 1: Pair of Jacks with ACE kicker {J,J,9,A,T}
        val hand1 =
            Hand(
                listOf(
                    Dice(Face.JACK),
                    Dice(Face.JACK),
                    Dice(Face.NINE),
                    Dice(Face.ACE),
                    Dice(Face.TEN),
                ),
            )

        // Player 2: Pair of Jacks with KING kicker {J,9,J,Q,K}
        val hand2 =
            Hand(
                listOf(
                    Dice(Face.JACK),
                    Dice(Face.NINE),
                    Dice(Face.JACK),
                    Dice(Face.QUEEN),
                    Dice(Face.KING),
                ),
            )

        val rank1 = defineHandRank(hand1)
        val rank2 = defineHandRank(hand2)

        val value1 = calculateFullHandValue(rank1)
        val value2 = calculateFullHandValue(rank2)

        // Player 1: 2*1000 + 3*10 (JACK) + 6 (ACE kicker) = 2036
        // Player 2: 2*1000 + 3*10 (JACK) + 5 (KING kicker) = 2035
        assertTrue(value1 > value2, "Pair of Jacks with ACE kicker ($value1) should beat pair of Jacks with KING kicker ($value2)")

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
        assertEquals(player1, winners[0], "Player 1 with higher kicker (ACE) should win")
    }
}
