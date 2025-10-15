import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

class EitherTests {
    @Test
    fun `test success creates Success instance`() {
        val result = success(42)
        assertTrue(result is Either.Success)
        assertEquals(42, (result as Either.Success).value)
    }

    @Test
    fun `test failure creates Failure instance`() {
        val result = failure("error message")
        assertTrue(result is Either.Failure)
        assertEquals("error message", (result as Either.Failure).value)
    }

    @Test
    fun `test Success with different types`() {
        val stringSuccess = success("hello")
        assertTrue(stringSuccess is Either.Success)
        assertEquals("hello", (stringSuccess as Either.Success).value)

        val intSuccess = success(100)
        assertTrue(intSuccess is Either.Success)
        assertEquals(100, (intSuccess as Either.Success).value)

        val booleanSuccess = success(true)
        assertTrue(booleanSuccess is Either.Success)
        assertEquals(true, (booleanSuccess as Either.Success).value)
    }

    @Test
    fun `test Failure with different types`() {
        val stringFailure = failure("error")
        assertTrue(stringFailure is Either.Failure)
        assertEquals("error", (stringFailure as Either.Failure).value)

        val intFailure = failure(404)
        assertTrue(intFailure is Either.Failure)
        assertEquals(404, (intFailure as Either.Failure).value)
    }

    @Test
    fun `test Either is sealed class`() {
        val success: Either<String, Int> = success(42)
        val failure: Either<String, Int> = failure("error")

        val successResult =
            when (success) {
                is Either.Success -> "success: ${success.value}"
                is Either.Failure -> "failure: ${success.value}"
            }
        assertEquals("success: 42", successResult)

        val failureResult =
            when (failure) {
                is Either.Success -> "success: ${failure.value}"
                is Either.Failure -> "failure: ${failure.value}"
            }
        assertEquals("failure: error", failureResult)
    }

    @Test
    fun `test Success equality`() {
        val success1 = success(42)
        val success2 = success(42)
        val success3 = success(43)

        assertEquals(success1, success2)
        assertTrue(success1 != success3)
    }

    @Test
    fun `test Failure equality`() {
        val failure1 = failure("error")
        val failure2 = failure("error")
        val failure3 = failure("different error")

        assertEquals(failure1, failure2)
        assertTrue(failure1 != failure3)
    }
}
