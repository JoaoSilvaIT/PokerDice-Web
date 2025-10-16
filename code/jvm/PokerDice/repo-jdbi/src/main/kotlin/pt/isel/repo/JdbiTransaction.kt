package pt.isel.repo

import org.jdbi.v3.core.Handle
import pt.isel.JdbiGamesRepository
import pt.isel.JdbiLobbiesRepository
import pt.isel.JdbiUsersRepository
import pt.isel.RepositoryGame
import pt.isel.RepositoryLobby
import pt.isel.RepositoryUser

class JdbiTransaction(
    private val handle: Handle,
) : Transaction {
    override val repoUsers: RepositoryUser = JdbiUsersRepository(handle)
    override val repoGame: RepositoryGame = JdbiGamesRepository(handle)
    override val repoLobby: RepositoryLobby = JdbiLobbiesRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}
