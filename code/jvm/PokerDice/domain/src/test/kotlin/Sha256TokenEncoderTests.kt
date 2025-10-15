import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import pt.isel.domain.users.Sha256TokenEncoder

class Sha256TokenEncoderTests {
    private val encoder = Sha256TokenEncoder()

    @Test
    fun `test createValidationInformation returns consistent hash for same token`() {
        val token = "my-secret-token"
        val validation1 = encoder.createValidationInformation(token)
        val validation2 = encoder.createValidationInformation(token)

        assertEquals(validation1.validationInfo, validation2.validationInfo)
    }

    @Test
    fun `test createValidationInformation returns different hash for different tokens`() {
        val token1 = "token1"
        val token2 = "token2"

        val validation1 = encoder.createValidationInformation(token1)
        val validation2 = encoder.createValidationInformation(token2)

        assertNotEquals(validation1.validationInfo, validation2.validationInfo)
    }

    @Test
    fun `test createValidationInformation with empty string`() {
        val token = ""
        val validation = encoder.createValidationInformation(token)

        assert(validation.validationInfo.isNotEmpty())
    }

    @Test
    fun `test createValidationInformation with special characters`() {
        val token = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`"
        val validation = encoder.createValidationInformation(token)

        assert(validation.validationInfo.isNotEmpty())
    }

    @Test
    fun `test createValidationInformation with unicode characters`() {
        val token = "こんにちは世界🌍"
        val validation = encoder.createValidationInformation(token)

        assert(validation.validationInfo.isNotEmpty())
    }

    @Test
    fun `test createValidationInformation hash is base64 encoded`() {
        val token = "test-token"
        val validation = encoder.createValidationInformation(token)

        // Base64 URL-encoded strings should only contain alphanumeric, '-', '_', and optionally '='
        val base64Pattern = Regex("^[A-Za-z0-9_-]+=*$")
        assert(base64Pattern.matches(validation.validationInfo))
    }

    @Test
    fun `test createValidationInformation produces different hash for similar tokens`() {
        val token1 = "token"
        val token2 = "Token"
        val token3 = "token "

        val validation1 = encoder.createValidationInformation(token1)
        val validation2 = encoder.createValidationInformation(token2)
        val validation3 = encoder.createValidationInformation(token3)

        assertNotEquals(validation1.validationInfo, validation2.validationInfo)
        assertNotEquals(validation1.validationInfo, validation3.validationInfo)
        assertNotEquals(validation2.validationInfo, validation3.validationInfo)
    }

    @Test
    fun `test createValidationInformation with long token`() {
        val token = "a".repeat(1000)
        val validation = encoder.createValidationInformation(token)

        assert(validation.validationInfo.isNotEmpty())
    }
}
