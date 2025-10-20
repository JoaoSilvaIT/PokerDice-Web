package pt.isel.repo

import org.jdbi.v3.core.Handle
import pt.isel.*

class JdbiTransaction(
    private val handle: Handle,
) : Transaction {
    override val repoUsers: RepositoryUser = JdbiUsersRepository(handle)
    override val repoGame: RepositoryGame = JdbiGamesRepository(handle)
    override val repoLobby: RepositoryLobby = JdbiLobbiesRepository(handle)
    override val repoInvite: RepositoryInvite = JdbiInviteRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}
