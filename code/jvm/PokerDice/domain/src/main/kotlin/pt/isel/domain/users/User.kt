package pt.isel.domain.users

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val balance: Int,
    val passwordValidation: PasswordValidationInfo,
)
