import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.utils.Face

class FaceTests {
    @Test
    fun `test Face strength values are correct`() {
        assertEquals(6, Face.ACE.strength)
        assertEquals(5, Face.KING.strength)
        assertEquals(4, Face.QUEEN.strength)
        assertEquals(3, Face.JACK.strength)
        assertEquals(2, Face.TEN.strength)
        assertEquals(1, Face.NINE.strength)
    }

    @Test
    fun `test Face ordering by strength`() {
        assertTrue(Face.ACE.strength > Face.KING.strength)
        assertTrue(Face.KING.strength > Face.QUEEN.strength)
        assertTrue(Face.QUEEN.strength > Face.JACK.strength)
        assertTrue(Face.JACK.strength > Face.TEN.strength)
        assertTrue(Face.TEN.strength > Face.NINE.strength)
    }

    @Test
    fun `test all Face values exist`() {
        val allFaces = Face.entries.toTypedArray()
        assertEquals(6, allFaces.size)
    }

    @Test
    fun `test Face enum names`() {
        assertEquals("ACE", Face.ACE.name)
        assertEquals("KING", Face.KING.name)
        assertEquals("QUEEN", Face.QUEEN.name)
        assertEquals("JACK", Face.JACK.name)
        assertEquals("TEN", Face.TEN.name)
        assertEquals("NINE", Face.NINE.name)
    }

    @Test
    fun `test Face valueOf`() {
        assertEquals(Face.ACE, Face.valueOf("ACE"))
        assertEquals(Face.KING, Face.valueOf("KING"))
        assertEquals(Face.QUEEN, Face.valueOf("QUEEN"))
        assertEquals(Face.JACK, Face.valueOf("JACK"))
        assertEquals(Face.TEN, Face.valueOf("TEN"))
        assertEquals(Face.NINE, Face.valueOf("NINE"))
    }
}
