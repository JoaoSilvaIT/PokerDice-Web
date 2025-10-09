package pt.isel.domain

data class Turn(
    val user: User,
    val rolls: List<Hand>,
) {
    init {
        require(rolls.size <= 3) { "A turn can have at most 3 rolls." }
    }
}
