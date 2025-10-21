package pt.isel.model.user

class UserOutputModel(
    val name: String,
    val balance: Int,
) {
    companion object {
        fun fromDomain(user: pt.isel.domain.users.User): UserOutputModel =
            UserOutputModel(
                name = user.name,
                balance = user.balance,
            )
    }
}
