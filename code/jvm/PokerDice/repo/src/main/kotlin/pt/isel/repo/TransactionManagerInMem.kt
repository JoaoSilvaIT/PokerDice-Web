package pt.isel.repo

import pt.isel.mem.RepositoryGameInMem
import pt.isel.mem.RepositoryLobbyInMem
import pt.isel.mem.RepositoryUserInMem

/**
 * In-memory transaction manager for testing purposes.
 * Does not provide true transactional semantics but allows tests to run without a database.
 */
class TransactionManagerInMem {
    private val repoUsers = RepositoryUserInMem()
    private val repoLobby = RepositoryLobbyInMem()
    private val repoGame = RepositoryGameInMem()

    fun <R> run(block: Transaction.() -> R): R {
        val transaction =
            Transaction(
                repoUsers = repoUsers,
                repoLobby = repoLobby,
                repoGame = repoGame,
            )
        return transaction.block()
    }
}
