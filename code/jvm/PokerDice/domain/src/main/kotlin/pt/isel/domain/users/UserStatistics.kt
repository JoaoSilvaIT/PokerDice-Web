package pt.isel.domain.users

data class UserStatistics(
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double
)
