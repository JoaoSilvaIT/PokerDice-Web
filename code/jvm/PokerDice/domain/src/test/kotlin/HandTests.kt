import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Hand
import pt.isel.utils.Face

class HandTests {
    @Test
    fun `test Hand creation with specific dices`() {
        val dices =
            listOf(
                Dice(Face.ACE),
                Dice(Face.KING),
                Dice(Face.QUEEN),
                Dice(Face.JACK),
                Dice(Face.TEN),
            )
        val hand = Hand(dices)
        assertEquals(5, hand.dices.size)
        assertEquals(dices, hand.dices)
    }

    @Test
    fun `test Hand rollDices creates 5 dices`() {
        val hand = Hand.rollDices()
        assertEquals(5, hand.dices.size)
    }

    @Test
    fun `test Hand rollDices returns valid faces`() {
        val hand = Hand.rollDices()
        hand.dices.forEach { dice ->
            assertTrue(Face.entries.contains(dice.face))
        }
    }

    @Test
    fun `test Hand rollDices randomness`() {
        val hands = List(20) { Hand.rollDices() }
        val uniqueHands = hands.map { it.dices.map { dice -> dice.face } }.toSet()

        // With 20 rolls, we should get more than 1 unique hand (statistically very likely)
        assertTrue(uniqueHands.size > 1, "Expected multiple unique hands in 20 rolls")
    }

    @Test
    fun `test Hand equality`() {
        val dices1 = List(5) { Dice(Face.ACE) }
        val dices2 = List(5) { Dice(Face.ACE) }
        val dices3 = List(5) { Dice(Face.KING) }

        val hand1 = Hand(dices1)
        val hand2 = Hand(dices2)
        val hand3 = Hand(dices3)

        assertEquals(hand1, hand2)
        assertTrue(hand1 != hand3)
    }

    @Test
    fun `test Hand with empty dices list`() {
        val hand = Hand(emptyList())
        assertEquals(0, hand.dices.size)
    }

    @Test
    fun `test Hand copy`() {
        val dices = List(5) { Dice(Face.ACE) }
        val hand1 = Hand(dices)
        val hand2 = hand1.copy()

        assertEquals(hand1, hand2)
    }

    @Test
    fun `test Hand with different number of dices`() {
        val hand3Dices = Hand(List(3) { Dice(Face.ACE) })
        val hand7Dices = Hand(List(7) { Dice(Face.KING) })

        assertEquals(3, hand3Dices.dices.size)
        assertEquals(7, hand7Dices.dices.size)
    }
}
