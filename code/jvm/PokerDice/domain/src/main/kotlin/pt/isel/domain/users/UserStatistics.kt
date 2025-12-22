package pt.isel.domain.users

import pt.isel.domain.games.utils.HandRank

data class UserStatistics(
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val handFrequencies: Map<HandRank, Int>,
)
