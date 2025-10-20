package pt.isel

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.users.InviteValidationInfo
import java.time.Instant

class JdbiInviteRepository(
    private val handle: Handle,
    ): RepositoryInvite {

    override fun createAppInvite(
        inviterId: Int,
        inviteValidationInfo: InviteValidationInfo,
        state: String,
        createdAt: Instant,
    ): String? =
        handle
            .createUpdate(
                "insert into dbo.app_invite(inviterid, invitevalidationinfo, state, createdat) " +
                        "values (:inviterId, :inviteValidationInfo, :state, :createdAt)",
            ).bind("inviterId", inviterId)
            .bind("inviteValidationInfo", inviteValidationInfo.validationInfo)
            .bind("state", state)
            .bind("createdAt", createdAt.epochSecond)
            .executeAndReturnGeneratedKeys()
            .mapTo<String>()
            .one()
}