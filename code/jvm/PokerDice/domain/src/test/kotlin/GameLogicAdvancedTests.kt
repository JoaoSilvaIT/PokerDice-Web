import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.domain.games.GameLogic
import pt.isel.domain.games.Hand
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.utils.Face
import pt.isel.utils.HandRank

class GameLogicAdvancedTests {
    private val gameLogic = GameLogic()
    private val passwordValidation = PasswordValidationInfo("hashed_password")

    @Test
    fun `test calculateFullHandValue with five of a kind`() {
        val hand = Hand(List(5) { Dice(Face.ACE) })
        val handRank = gameLogic.defineHandRank(hand)
        val value = gameLogic.calculateFullHandValue(handRank)

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
        val fourAcesRank = gameLogic.defineHandRank(fourAces)
        val fourAcesValue = gameLogic.calculateFullHandValue(fourAcesRank)
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
        val fullHouseRank = gameLogic.defineHandRank(fullHouse)
        val fullHouseValue = gameLogic.calculateFullHandValue(fullHouseRank)
        assertEquals(11, fullHouseValue) // 6 + 5
    }

    @Test
    fun `test decideRoundWinner with single winner`() {
        val user1 = User(1, "Alice", "alice@example.com", 100, passwordValidation)
        val user2 = User(2, "Bob", "bob@example.com", 100, passwordValidation)

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
                turn = Turn(user1),
                users = listOf(user1, user2),
                userHands = mapOf(user1 to hand1, user2 to hand2),
            )

        val winners = gameLogic.decideRoundWinner(round)

        assertEquals(1, winners.size)
        assertEquals(user1, winners[0])
    }

    @Test
    fun `test decideRoundWinner with tie`() {
        val user1 = User(1, "Alice", "alice@example.com", 100, passwordValidation)
        val user2 = User(2, "Bob", "bob@example.com", 100, passwordValidation)

        val hand1 = Hand(List(5) { Dice(Face.ACE) })
        val hand2 = Hand(List(5) { Dice(Face.ACE) })

        val round =
            Round(
                number = 1,
                turn = Turn(user1),
                users = listOf(user1, user2),
                userHands = mapOf(user1 to hand1, user2 to hand2),
            )

        val winners = gameLogic.decideRoundWinner(round)

        assertEquals(2, winners.size)
        assertTrue(winners.contains(user1))
        assertTrue(winners.contains(user2))
    }

    @Test
    fun `test decideRoundWinner with multiple players`() {
        val user1 = User(1, "Alice", "alice@example.com", 100, passwordValidation)
        val user2 = User(2, "Bob", "bob@example.com", 100, passwordValidation)
        val user3 = User(3, "Charlie", "charlie@example.com", 100, passwordValidation)

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
                turn = Turn(user1),
                users = listOf(user1, user2, user3),
                userHands = mapOf(user1 to hand1, user2 to hand2, user3 to hand3),
            )

        val winners = gameLogic.decideRoundWinner(round)

        assertEquals(1, winners.size)
        assertEquals(user2, winners[0])
    }

    @Test
    fun `test decideRoundWinner with same rank but different high card`() {
        val user1 = User(1, "Alice", "alice@example.com", 100, passwordValidation)
        val user2 = User(2, "Bob", "bob@example.com", 100, passwordValidation)

        val hand1 = Hand(List(5) { Dice(Face.KING) }) // Five of a kind KING
        val hand2 = Hand(List(5) { Dice(Face.ACE) }) // Five of a kind ACE (winner)

        val round =
            Round(
                number = 1,
                turn = Turn(user1),
                users = listOf(user1, user2),
                userHands = mapOf(user1 to hand1, user2 to hand2),
            )

        val winners = gameLogic.decideRoundWinner(round)

        assertEquals(1, winners.size)
        assertEquals(user2, winners[0])
    }

    @Test
    fun `test defineHandRank edge case - straight without nine`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                    Dice(Face.JACK),
                    Dice(Face.TEN),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.STRAIGHT, rank)
    }

    @Test
    fun `test defineHandRank edge case - not a straight with nine`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.NINE),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                    Dice(Face.JACK),
                    Dice(Face.TEN),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.HIGH_DICE, rank)
    }

    @Test
    fun `test calculateFullHandValue consistency`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                    Dice(Face.JACK),
                ),
            )
        val handRank = gameLogic.defineHandRank(hand)
        val value1 = gameLogic.calculateFullHandValue(handRank)
        val value2 = gameLogic.calculateFullHandValue(handRank)

        assertEquals(value1, value2)
    }
}
