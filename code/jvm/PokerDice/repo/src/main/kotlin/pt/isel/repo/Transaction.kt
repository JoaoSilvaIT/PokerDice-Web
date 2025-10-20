package pt.isel.repo

import pt.isel.RepositoryGame
import pt.isel.RepositoryInvite
import pt.isel.RepositoryLobby
import pt.isel.RepositoryUser

interface Transaction {
    val repoUsers: RepositoryUser
    val repoLobby: RepositoryLobby
    val repoGame: RepositoryGame
    val repoInvite: RepositoryInvite

    fun rollback()
}
