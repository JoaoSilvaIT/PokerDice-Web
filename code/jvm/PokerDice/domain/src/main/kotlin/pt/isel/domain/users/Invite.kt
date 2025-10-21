package pt.isel.domain.users

import java.time.Instant

class Invite(
    val id: Int,
    val inviterId: Int,
    val inviteValidationInfo: InviteValidationInfo,
    val state: String,
    val createdAt: Instant,
)
