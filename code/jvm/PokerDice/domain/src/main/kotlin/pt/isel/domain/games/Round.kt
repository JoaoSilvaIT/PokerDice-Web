package pt.isel.domain.games

import pt.isel.domain.users.User
import pt.isel.domain.users.UserExternalInfo

const val MIN_ANTE = 10

data class Round(
    // round number in the game
    val number: Int,
    val firstPlayerIdx: Int,
    val turn: Turn,
    val players: Set<UserExternalInfo>,
    val userHands: Map<User, Hand>,
    val ante: Int = MIN_ANTE,
    val pot : Int = 0,
    val winner: UserExternalInfo? = null,
    val gameId : Int? = null,
) {
    /*
    fun setAnte(newAnte: Int): Round {
        require(newAnte > 0 && newAnte >= MIN_ANTE) { "Cost must be positive and at least $MIN_ANTE" }
        return this.copy(ante = newAnte)
    }

    fun payAnte(): Round {
        val updatedUsers =
            users.map { user ->
                require(user.balance >= ante) {
                    "User ${user.name} has insufficient balance (${user.balance}) to pay ante ($ante)"
                }
                user.copy(balance = user.balance - ante)
            }
        return this.copy(users = updatedUsers)
    }

    fun startNewRound(users : List<User>, round: Round?): Round {
        val nextRoundNr = (round?.number ?: 0) + 1
        val firstPlayerIndex = (nextRoundNr - 1) % users.size
        return Round(
            number = nextRoundNr,
            firstPlayerIdx = firstPlayerIndex,
            turn = Turn(users[firstPlayerIndex], Hand(emptyList())),
            users = users,
            userHands = emptyMap())
    }

    fun nextTurn(round: Round): Round {
        val currentIndex = round.users.indexOf(round.turn.user)
        val updatedUserHands = round.userHands + (round.turn.user to round.turn.hand)
        return if (((currentIndex + 1) % round.users.size) == round.firstPlayerIdx) {
            startNewRound(round.users, round)
        } else this.copy(
            turn = Turn(round.users[(currentIndex + 1) % round.users.size]),
            userHands = updatedUserHands,
        )
    }

     */
}
