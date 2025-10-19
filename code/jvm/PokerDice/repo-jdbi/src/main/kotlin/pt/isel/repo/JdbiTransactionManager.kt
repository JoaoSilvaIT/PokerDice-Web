package pt.isel.repo

import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("jdbi")
class JdbiTransactionManager(
    private val jdbi: Jdbi,
) : TransactionManager {
    override fun <R> run(block: (Transaction) -> R): R =
        jdbi.inTransaction<R, Exception> { handle ->
            val transaction = JdbiTransaction(handle)
            block(transaction)
        }
}

