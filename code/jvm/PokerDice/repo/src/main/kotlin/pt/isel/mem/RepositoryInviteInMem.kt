package pt.isel.mem

import pt.isel.RepositoryInvite
import pt.isel.domain.users.InviteValidationInfo

class RepositoryInviteInMem: RepositoryInvite {

    override fun createAppInvite(
        inviterId: Int,
        inviteValidationInfo: InviteValidationInfo,
        state: String,
        createdAt: java.time.Instant
    ): String? {
        TODO("Not yet implemented")
    }
}