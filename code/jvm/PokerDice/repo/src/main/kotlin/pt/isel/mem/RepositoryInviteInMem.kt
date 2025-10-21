package pt.isel.mem

import pt.isel.RepositoryInvite
import pt.isel.domain.users.Invite
import pt.isel.domain.users.InviteValidationInfo
import java.time.Instant

class RepositoryInviteInMem : RepositoryInvite {
    private val invites = mutableMapOf<Int, Invite>()
    private var nextId = 1

    override fun createAppInvite(
        inviterId: Int,
        inviteValidationInfo: InviteValidationInfo,
        state: String,
        createdAt: Instant,
    ): String? {
        val id = nextId++
        val invite =
            Invite(
                id = id,
                inviterId = inviterId,
                inviteValidationInfo = inviteValidationInfo,
                state = state,
                createdAt = createdAt,
            )
        invites[id] = invite
        return inviteValidationInfo.validationInfo
    }

    override fun changeInviteState(
        inviteId: Int,
        state: String,
    ): Int {
        val invite = invites[inviteId] ?: return 0
        invites[inviteId] =
            Invite(
                id = invite.id,
                inviterId = invite.inviterId,
                inviteValidationInfo = invite.inviteValidationInfo,
                state = state,
                createdAt = invite.createdAt,
            )
        return 1
    }

    override fun getAppInviteByValidationInfo(inviteValidationInfo: InviteValidationInfo): Invite? {
        return invites.values.find {
            it.inviteValidationInfo.validationInfo == inviteValidationInfo.validationInfo
        }
    }

    fun clear() {
        invites.clear()
        nextId = 1
    }
}
