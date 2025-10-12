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

    fun payAnte() : Round {
        val updatedUsers = users.map { user ->
            user.copy(balance = user.balance - ante)
        }
        return this.copy(users = updatedUsers)
    }

}
