package pt.isel

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.users.Invite
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
                "insert into dbo.INVITE(inviterid, invitevalidationinfo, state, createdat) " +
                        "values (:inviterId, :inviteValidationInfo, :state, :createdAt)",
            ).bind("inviterId", inviterId)
            .bind("inviteValidationInfo", inviteValidationInfo.validationInfo)
            .bind("state", state)
            .bind("createdAt", createdAt.epochSecond)
            .executeAndReturnGeneratedKeys()
            .mapTo<String>()
            .one()

    override fun getAppInviteByValidationInfo(inviteValidationInfo: InviteValidationInfo): Invite? =
        handle
            .createQuery("select * from dbo.INVITE where invitevalidationinfo = :inviteValidationInfo")
            .bind("inviteValidationInfo", inviteValidationInfo.validationInfo)
            .map { rs, _ ->
                Invite(
                    id = rs.getInt("id"),
                    inviterId = rs.getInt("inviterid"),
                    inviteValidationInfo = InviteValidationInfo(rs.getString("invitevalidationinfo")),
                    state = rs.getString("state"),
                    createdAt = Instant.ofEpochSecond(rs.getLong("createdat"))
                )
            }
            .singleOrNull()

    override fun changeInviteState(
        inviteId: Int,
        state: String,
    ): Int {
        return handle
            .createUpdate("update dbo.INVITE set state = :state where id = :id")
            .bind("state", state)
            .bind("id", inviteId)
            .execute()
    }



}