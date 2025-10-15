import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User

class UserTests {
    @Test
    fun `test User creation with all parameters`() {
        val passwordValidation = PasswordValidationInfo("hashed_password")
        val user =
            User(
                id = 1,
                name = "John Doe",
                email = "john@example.com",
                balance = 200,
                passwordValidation = passwordValidation,
            )

        assertEquals(1, user.id)
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals(200, user.balance)
        assertEquals(passwordValidation, user.passwordValidation)
    }

    @Test
    fun `test User creation with default balance`() {
        val passwordValidation = PasswordValidationInfo("hashed_password")
        val user =
            User(
                id = 2,
                name = "Jane Doe",
                email = "jane@example.com",
                passwordValidation = passwordValidation,
            )

        assertEquals(100, user.balance)
    }

    @Test
    fun `test User copy with updated balance`() {
        val passwordValidation = PasswordValidationInfo("hashed_password")
        val user1 =
            User(
                id = 1,
                name = "John Doe",
                email = "john@example.com",
                balance = 100,
                passwordValidation = passwordValidation,
            )

        val user2 = user1.copy(balance = 150)

        assertEquals(1, user2.id)
        assertEquals("John Doe", user2.name)
        assertEquals(150, user2.balance)
    }

    @Test
    fun `test User equality`() {
        val passwordValidation = PasswordValidationInfo("hashed_password")
        val user1 = User(1, "John", "john@example.com", 100, passwordValidation)
        val user2 = User(1, "John", "john@example.com", 100, passwordValidation)
        val user3 = User(2, "Jane", "jane@example.com", 100, passwordValidation)

        assertEquals(user1, user2)
        assert(user1 != user3)
    }

    @Test
    fun `test User with zero balance`() {
        val passwordValidation = PasswordValidationInfo("hashed_password")
        val user =
            User(
                id = 3,
                name = "Broke User",
                email = "broke@example.com",
                balance = 0,
                passwordValidation = passwordValidation,
            )

        assertEquals(0, user.balance)
    }

    @Test
    fun `test User with negative balance`() {
        val passwordValidation = PasswordValidationInfo("hashed_password")
        val user =
            User(
                id = 4,
                name = "Debt User",
                email = "debt@example.com",
                balance = -50,
                passwordValidation = passwordValidation,
            )

        assertEquals(-50, user.balance)
    }
}
