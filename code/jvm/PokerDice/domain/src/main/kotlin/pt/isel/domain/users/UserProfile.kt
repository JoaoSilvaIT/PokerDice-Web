package pt.isel.domain.users

const val MIN_BALANCE = 100

data class UserProfile(
    val id: Int,
    val name: String,
    val balance: Int,
    val statistics: UserStatistics,
)
