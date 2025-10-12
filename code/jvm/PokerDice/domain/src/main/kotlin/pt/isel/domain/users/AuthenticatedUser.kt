package pt.isel.domain.users

class AuthenticatedUser(
    val user: User,
    val token: String,
)
