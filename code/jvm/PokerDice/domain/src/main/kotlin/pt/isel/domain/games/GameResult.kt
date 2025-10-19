package pt.isel.domain.games

data class GameResult(
    val id: Int,
    val winnerId: Int,
    val playerResults: List<PlayerResult>
)
