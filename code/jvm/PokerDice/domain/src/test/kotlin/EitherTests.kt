import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pt.isel.utils.Either
import pt.isel.utils.failure
import pt.isel.utils.success

class EitherTests {
    // Helper functions to prevent compiler from knowing exact types
    private fun <F, S> createSuccess(value: S): Either<F, S> = success(value)

    private fun <F, S> createFailure(error: F): Either<F, S> = failure(error)

    @Test
    fun `test success creates Success instance`() {
        when (val result = createSuccess<String, Int>(42)) {
            is Either.Success -> assertEquals(42, result.value)
            is Either.Failure -> throw AssertionError("Expected Success but got Failure")
        }
    }

    @Test
    fun `test failure creates Failure instance`() {
        when (val result = createFailure<String, Int>("error message")) {
            is Either.Success -> throw AssertionError("Expected Failure but got Success")
            is Either.Failure -> assertEquals("error message", result.value)
        }
    }

    @Test
    fun `test Success with different types`() {
        when (val stringSuccess = createSuccess<Int, String>("hello")) {
            is Either.Success -> assertEquals("hello", stringSuccess.value)
            is Either.Failure -> throw AssertionError("Expected Success")
        }

        when (val intSuccess = createSuccess<String, Int>(100)) {
            is Either.Success -> assertEquals(100, intSuccess.value)
            is Either.Failure -> throw AssertionError("Expected Success")
        }

        when (val booleanSuccess = createSuccess<String, Boolean>(true)) {
            is Either.Success -> assertEquals(true, booleanSuccess.value)
            is Either.Failure -> throw AssertionError("Expected Success")
        }
    }

    @Test
    fun `test Failure with different types`() {
        when (val stringFailure = createFailure<String, Int>("error")) {
            is Either.Success -> throw AssertionError("Expected Failure")
            is Either.Failure -> assertEquals("error", stringFailure.value)
        }

        when (val intFailure = createFailure<Int, String>(404)) {
            is Either.Success -> throw AssertionError("Expected Failure")
            is Either.Failure -> assertEquals(404, intFailure.value)
        }
    }

    @Test
    fun `test Either is sealed class`() {
        val successEither = createSuccess<String, Int>(42)
        val failureEither = createFailure<String, Int>("error")

        val successResult =
            when (successEither) {
                is Either.Success -> "success: ${successEither.value}"
                is Either.Failure -> "failure: ${successEither.value}"
            }
        assertEquals("success: 42", successResult)

        val failureResult =
            when (failureEither) {
                is Either.Success -> "success: ${failureEither.value}"
                is Either.Failure -> "failure: ${failureEither.value}"
            }
        assertEquals("failure: error", failureResult)
    }

    @Test
    fun `test Success equality`() {
        val success1: Either<String, Int> = success(42)
        val success2: Either<String, Int> = success(42)
        val success3: Either<String, Int> = success(43)

        assertEquals(success1, success2)
        assertTrue(success1 != success3)
    }

    @Test
    fun `test Failure equality`() {
        val failure1: Either<String, Int> = failure("error")
        val failure2: Either<String, Int> = failure("error")
        val failure3: Either<String, Int> = failure("different error")

        assertEquals(failure1, failure2)
        assertTrue(failure1 != failure3)
    }
}
