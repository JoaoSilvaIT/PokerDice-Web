import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pt.isel.domain.games.Dice
import pt.isel.domain.games.Hand
import pt.isel.domain.games.MIN_ANTE
import pt.isel.domain.games.Round
import pt.isel.domain.games.Turn
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.domain.games.utils.Face

class RoundTests {
    private val passwordValidation = PasswordValidationInfo("hashed_password")
    private val user1 = User(1, "Alice", "alice@example.com", 100, passwordValidation)
    private val user2 = User(2, "Bob", "bob@example.com", 100, passwordValidation)
    private val users = listOf(user1, user2)

    @Test
    fun `test Round creation with default ante`() {
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(user1),
                users = users,
                userHands = emptyMap(),
            )

        assertEquals(1, round.number)
        assertEquals(user1, round.turn.user)
        assertEquals(2, round.users.size)
        assertEquals(MIN_ANTE, round.ante)
    }

    @Test
    fun `test Round creation with custom ante`() {
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(user1),
                users = users,
                userHands = emptyMap(),
                ante = 20,
            )

        assertEquals(20, round.ante)
    }

    @Test
    fun `test setAnte updates ante correctly`() {
        val round = Round(number = 1, firstPlayerIdx = 0, turn = Turn(user1), users = users, userHands = emptyMap())
        val updatedRound = round.setAnte(50)

        assertEquals(50, updatedRound.ante)
    }

    @Test
    fun `test setAnte with minimum ante`() {
        val round = Round(number = 1, firstPlayerIdx = 0, turn = Turn(user1), users = users, userHands = emptyMap())
        val updatedRound = round.setAnte(MIN_ANTE)

        assertEquals(MIN_ANTE, updatedRound.ante)
    }

    @Test
    fun `test setAnte throws exception for ante below minimum`() {
        val round = Round(number = 1, firstPlayerIdx = 0, turn = Turn(user1), users = users, userHands = emptyMap())

        assertThrows(IllegalArgumentException::class.java) {
            round.setAnte(MIN_ANTE - 1)
        }
    }

    @Test
    fun `test setAnte throws exception for zero ante`() {
        val round = Round(number = 1, firstPlayerIdx = 0, turn = Turn(user1), users = users, userHands = emptyMap())

        assertThrows(IllegalArgumentException::class.java) {
            round.setAnte(0)
        }
    }

    @Test
    fun `test setAnte throws exception for negative ante`() {
        val round = Round(number = 1, firstPlayerIdx = 1, turn = Turn(user1), users = users, userHands = emptyMap())

        assertThrows(IllegalArgumentException::class.java) {
            round.setAnte(-10)
        }
    }

    @Test
    fun `test payAnte deducts ante from all users`() {
        val round = Round(number = 1, firstPlayerIdx = 1, turn = Turn(user1), users = users, userHands = emptyMap(), ante = 20)
        val updatedRound = round.payAnte()

        assertEquals(80, updatedRound.users[0].balance)
        assertEquals(80, updatedRound.users[1].balance)
    }

    @Test
    fun `test payAnte with default ante`() {
        val round = Round(number = 1, firstPlayerIdx = 1, turn = Turn(user1), users = users, userHands = emptyMap())
        val updatedRound = round.payAnte()

        assertEquals(90, updatedRound.users[0].balance)
        assertEquals(90, updatedRound.users[1].balance)
    }

    @Test
    fun `test payAnte throws exception when user has insufficient balance`() {
        val poorUser = User(3, "Poor", "poor@example.com", 5, passwordValidation)
        val round = Round(number =1, firstPlayerIdx = 1, turn = Turn(poorUser), users = listOf(poorUser), userHands = emptyMap(), ante = 20)

        assertThrows(IllegalArgumentException::class.java) {
            round.payAnte()
        }
    }

    @Test
    fun `test payAnte with exact balance`() {
        val exactUser = User(3, "Exact", "exact@example.com", MIN_ANTE, passwordValidation)
        val round = Round(number = 1, firstPlayerIdx = 1, turn = Turn(exactUser), users = listOf(exactUser), userHands = emptyMap())
        val updatedRound = round.payAnte()

        assertEquals(0, updatedRound.users[0].balance)
    }

    @Test
    fun `test nextTurn updates turn to next player`() {
        val hand = Hand(List(5) { Dice(Face.ACE) })
        val round =
            Round(
                number = 1,
                firstPlayerIdx = 0,
                turn = Turn(user1, hand),
                users = users,
                userHands = emptyMap(),
            )

        val nextRound = round.nextTurn(round)

        assertEquals(1, nextRound.number)
        assertEquals(user2, nextRound.turn.user)
        assertEquals(1, nextRound.userHands.size)
        assertEquals(hand, nextRound.userHands[user1])
    }

    @Test
    fun `test nextTurn cycles back to first player`() {
        val hand = Hand(List(5) { Dice(Face.KING) })
        val round =
            Round(
                number = 2,
                firstPlayerIdx = 0,
                turn = Turn(user2, hand),
                users = users,
                userHands = mapOf(user1 to Hand(List(5) { Dice(Face.ACE) })),
            )

        val nextRound = round.nextTurn(round)

        assertEquals(3, nextRound.number)
        assertEquals(user1, nextRound.turn.user)
    }

    @Test
    fun `test Round equality`() {
        val round1 = Round(number = 1, firstPlayerIdx = 1, turn = Turn(user1), users = users, userHands = emptyMap())
        val round2 = Round(number = 1,  firstPlayerIdx = 1, turn = Turn(user1), users = users, userHands = emptyMap())
        val round3 = Round(number = 2, firstPlayerIdx = 1, turn = Turn(user2), users = users, userHands = emptyMap())

        assertEquals(round1, round2)
        assert(round1 != round3)
    }
}
