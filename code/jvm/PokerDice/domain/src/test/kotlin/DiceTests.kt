import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.utils.Face

class DiceTests {
    @Test
    fun `test Dice creation with specific face`() {
        val dice = Dice(Face.ACE)
        assertEquals(Face.ACE, dice.face)
    }

    @Test
    fun `test Dice creation with all faces`() {
        Face.entries.forEach { face ->
            val dice = Dice(face)
            assertEquals(face, dice.face)
        }
    }

    @Test
    fun `test Dice roll returns valid face`() {
        val dice = Dice.roll()
        assertTrue(Face.entries.contains(dice.face))
    }

    @Test
    fun `test Dice roll randomness`() {
        val rolls = List(100) { Dice.roll() }
        val uniqueFaces = rolls.map { it.face }.toSet()

        // With 100 rolls, we should get more than 1 unique face (statistically very likely)
        assertTrue(uniqueFaces.size > 1, "Expected multiple unique faces in 100 rolls")
    }

    @Test
    fun `test Dice equality`() {
        val dice1 = Dice(Face.KING)
        val dice2 = Dice(Face.KING)
        val dice3 = Dice(Face.QUEEN)

        assertEquals(dice1, dice2)
        assertTrue(dice1 != dice3)
    }

    @Test
    fun `test Dice copy`() {
        val dice1 = Dice(Face.ACE)
        val dice2 = dice1.copy()
        val dice3 = dice1.copy(face = Face.KING)

        assertEquals(dice1, dice2)
        assertEquals(Face.ACE, dice2.face)
        assertEquals(Face.KING, dice3.face)
        assertTrue(dice1 != dice3)
    }
}
