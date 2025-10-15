package pt.isel.repo

/**
 * Transaction manager interface for executing operations within a transactional context.
 */
interface TransactionManager {
    /**
     * Execute a block of code within a transaction.
     * The block receives a Transaction object with access to all repositories.
     */
    fun <R> run(block: Transaction.() -> R): R
}
