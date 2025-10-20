package pt.isel.mem

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import pt.isel.repo.Transaction
import pt.isel.repo.TransactionManager

/**
 * In-memory transaction manager for testing purposes.
 * Does not provide true transactional semantics but allows tests to run without a database.
 */
@Component
@Profile("mem")
@Primary
class TransactionManagerInMem : TransactionManager {
    private val repoUsers = RepositoryUserInMem()
    private val repoLobby = RepositoryLobbyInMem()
    private val repoGame = RepositoryGameInMem()
    private val repoInvite = RepositoryInviteInMem()

    override fun <R> run(block: Transaction.() -> R): R {
        val transaction =
            TransactionInMem(
                repoUsers = repoUsers,
                repoLobby = repoLobby,
                repoGame = repoGame,
                repoInvite = repoInvite
            )
        return transaction.block()
    }
}

class TransactionInMem(
    override val repoUsers: RepositoryUserInMem,
    override val repoLobby: RepositoryLobbyInMem,
    override val repoGame: RepositoryGameInMem,
    override val repoInvite: RepositoryInviteInMem
) : Transaction {
    override fun rollback() {
    }
}
