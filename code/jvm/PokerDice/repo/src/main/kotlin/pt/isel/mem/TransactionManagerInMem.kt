package pt.isel.mem

import pt.isel.repo.Transaction
import pt.isel.repo.TransactionManager

/**
 * In-memory transaction manager for testing purposes.
 * Does not provide true transactional semantics but allows tests to run without a database.
 */
class TransactionManagerInMem : TransactionManager {
    private val repoUsers = RepositoryUserInMem()
    private val repoLobby = RepositoryLobbyInMem()
    private val repoGame = RepositoryGameInMem()

    override fun <R> run(block: Transaction.() -> R): R {
        val transaction =
            Transaction(
                repoUsers = repoUsers,
                repoLobby = repoLobby,
                repoGame = repoGame,
            )
        return transaction.block()
    }
}
