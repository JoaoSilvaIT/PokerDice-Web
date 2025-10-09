package pt.isel.domain

data class Turn(
    val user: User,
    val rolls: List<Hand>,
)
