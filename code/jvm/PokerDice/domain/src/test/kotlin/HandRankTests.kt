import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.games.utils.HandRank

class HandRankTests {
    @Test
    fun `test HandRank strength values are correct`() {
        assertEquals(8, HandRank.FIVE_OF_A_KIND.strength)
        assertEquals(7, HandRank.FOUR_OF_A_KIND.strength)
        assertEquals(6, HandRank.FULL_HOUSE.strength)
        assertEquals(5, HandRank.STRAIGHT.strength)
        assertEquals(4, HandRank.THREE_OF_A_KIND.strength)
        assertEquals(3, HandRank.TWO_PAIR.strength)
        assertEquals(2, HandRank.ONE_PAIR.strength)
        assertEquals(1, HandRank.HIGH_DICE.strength)
    }

    @Test
    fun `test HandRank ordering by strength`() {
        assertTrue(HandRank.FIVE_OF_A_KIND.strength > HandRank.FOUR_OF_A_KIND.strength)
        assertTrue(HandRank.FOUR_OF_A_KIND.strength > HandRank.FULL_HOUSE.strength)
        assertTrue(HandRank.FULL_HOUSE.strength > HandRank.STRAIGHT.strength)
        assertTrue(HandRank.STRAIGHT.strength > HandRank.THREE_OF_A_KIND.strength)
        assertTrue(HandRank.THREE_OF_A_KIND.strength > HandRank.TWO_PAIR.strength)
        assertTrue(HandRank.TWO_PAIR.strength > HandRank.ONE_PAIR.strength)
        assertTrue(HandRank.ONE_PAIR.strength > HandRank.HIGH_DICE.strength)
    }

    @Test
    fun `test all HandRank values exist`() {
        val allRanks = HandRank.entries.toTypedArray()
        assertEquals(8, allRanks.size)
    }

    @Test
    fun `test HandRank enum names`() {
        assertEquals("FIVE_OF_A_KIND", HandRank.FIVE_OF_A_KIND.name)
        assertEquals("FOUR_OF_A_KIND", HandRank.FOUR_OF_A_KIND.name)
        assertEquals("FULL_HOUSE", HandRank.FULL_HOUSE.name)
        assertEquals("STRAIGHT", HandRank.STRAIGHT.name)
        assertEquals("THREE_OF_A_KIND", HandRank.THREE_OF_A_KIND.name)
        assertEquals("TWO_PAIR", HandRank.TWO_PAIR.name)
        assertEquals("ONE_PAIR", HandRank.ONE_PAIR.name)
        assertEquals("HIGH_DICE", HandRank.HIGH_DICE.name)
    }
}
