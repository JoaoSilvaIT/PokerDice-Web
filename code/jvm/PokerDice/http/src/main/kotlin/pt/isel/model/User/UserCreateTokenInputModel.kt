package pt.isel.model.User

data class UserCreateTokenInputModel(
    val email: String,
    val password: String,
)