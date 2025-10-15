import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.domain.games.GameLogic
import pt.isel.domain.games.Hand
import pt.isel.utils.Face
import pt.isel.utils.HandRank

class GameLogicTests {
    val gameLogic = GameLogic()

    @Test
    fun `test five of a kind`() {
        val hand = Hand(List(5) { Dice(Face.ACE) })
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.FIVE_OF_A_KIND, rank)
    }

    @Test
    fun `test four of a kind`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.FOUR_OF_A_KIND, rank)
    }

    @Test
    fun `test full house`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                    Dice(Face.KING),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.FULL_HOUSE, rank)
    }

    @Test
    fun `test three of a kind`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.THREE_OF_A_KIND, rank)
    }

    @Test
    fun `test two pair`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.ACE),
                    Dice(Face.ACE),
                    Dice(Face.KING),
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.TWO_PAIR, rank)
    }

    @Test
    fun `test one pair`() {
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
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.ONE_PAIR, rank)
    }

    @Test
    fun `test high dice`() {
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
    fun `test straight`() {
        val hand =
            Hand(
                listOf(
                    Dice(Face.KING),
                    Dice(Face.QUEEN),
                    Dice(Face.JACK),
                    Dice(Face.TEN),
                    Dice(Face.ACE),
                ),
            )
        val (_, rank) = gameLogic.defineHandRank(hand)
        assertEquals(HandRank.STRAIGHT, rank)
    }
}
