package pt.isel

import pt.isel.domain.users.InviteValidationInfo
import java.time.Instant

interface RepositoryInvite {
    fun createAppInvite(
        inviterId: Int,
        inviteValidationInfo: InviteValidationInfo,
        state: String,
        createdAt: Instant,
    ): String?
}