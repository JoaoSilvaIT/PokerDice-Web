package pt.isel.model.user

data class UserCreateTokenInputModel(
    val email: String,
    val password: String,
)
