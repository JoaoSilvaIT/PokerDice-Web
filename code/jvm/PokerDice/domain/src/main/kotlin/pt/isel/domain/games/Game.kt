package pt.isel.domain.games

import pt.isel.domain.users.User
import pt.isel.domain.games.utils.State

data class Game(
    val id: Int,
    val lobbyId: Int,
    val players: List<PlayerInGame>,
    val numberOfRounds: Int,
    val state: State,
    val currentRound: Round?,
    val startedAt: Long,
    val endedAt: Long?,
    // List to register the gains of each user in each round to decide the final winner
    val gameGains: List<Pair<User, Int>> = emptyList(),
    ) {
    init {
        require(state == State.RUNNING && currentRound != null) {"If state is RUNNING, current round is not null"}
    }
}
