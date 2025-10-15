package pt.isel.repo

import pt.isel.RepositoryGame
import pt.isel.RepositoryLobby
import pt.isel.RepositoryUser

/**
 * Transaction context that provides access to repositories.
 * Repositories share the same transactional context (Handle for JDBI, or shared state for in-memory).
 */
class Transaction(
    val repoUsers: RepositoryUser,
    val repoLobby: RepositoryLobby,
    val repoGame: RepositoryGame,
)
