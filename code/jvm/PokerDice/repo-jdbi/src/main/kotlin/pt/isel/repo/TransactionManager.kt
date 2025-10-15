package pt.isel.repo

import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component
import pt.isel.JdbiGamesRepository
import pt.isel.JdbiLobbiesRepository
import pt.isel.JdbiUsersRepository

@Component
class TransactionManagerJdbi(
    private val jdbi: Jdbi,
) : TransactionManager {
    /**
     * Execute a block of code within a transaction.
     * The block receives a Transaction object with access to all repositories.
     */
    override fun <R> run(block: Transaction.() -> R): R =
        jdbi.inTransaction<R, Exception> { handle ->
            val transaction = Transaction(
                repoUsers = JdbiUsersRepository(handle),
                repoLobby = JdbiLobbiesRepository(handle),
                repoGame = JdbiGamesRepository(handle),
            )
            transaction.block()
        }
}
