package pt.isel.domain.games

import pt.isel.domain.users.User

const val MIN_ANTE = 10

data class Round(
    // round number in the game
    val number: Int,
    val turn: Turn,
    val users: List<User>,
    val userHands: Map<User, Hand>,
    val ante: Int = MIN_ANTE,
) {
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

    fun nextTurn(round: Round): Round {
        val currentIndex = round.users.indexOf(round.turn.user)
        val updatedUserHands = round.userHands + (round.turn.user to round.turn.hand)
        return this.copy(
            turn = Turn(round.users[(currentIndex + 1) % round.users.size]),
            userHands = updatedUserHands,
        )
    }
}
