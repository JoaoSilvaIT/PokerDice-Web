package pt.isel.mem

import pt.isel.RepositoryInvite
import pt.isel.domain.users.Invite
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

    override fun changeInviteState(inviteId: Int, state: String): Int {
        TODO("Not yet implemented")
    }

    override fun getAppInviteByValidationInfo(inviteValidationInfo: InviteValidationInfo): Invite? {
        TODO("Not yet implemented")
    }
}