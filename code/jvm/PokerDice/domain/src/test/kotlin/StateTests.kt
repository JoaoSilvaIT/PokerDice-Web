import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.isel.utils.State

class StateTests {
    @Test
    fun `test all State values exist`() {
        val allStates = State.entries.toTypedArray()
        assertEquals(4, allStates.size)
    }

    @Test
    fun `test State enum names`() {
        assertEquals("RUNNING", State.RUNNING.name)
        assertEquals("TERMINATED", State.TERMINATED.name)
        assertEquals("WAITING", State.WAITING.name)
        assertEquals("FINISHED", State.FINISHED.name)
    }

    @Test
    fun `test State valueOf`() {
        assertEquals(State.RUNNING, State.valueOf("RUNNING"))
        assertEquals(State.TERMINATED, State.valueOf("TERMINATED"))
        assertEquals(State.WAITING, State.valueOf("WAITING"))
        assertEquals(State.FINISHED, State.valueOf("FINISHED"))
    }
}
