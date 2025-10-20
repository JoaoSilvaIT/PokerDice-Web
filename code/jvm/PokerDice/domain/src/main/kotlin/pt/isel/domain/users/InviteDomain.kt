package pt.isel.domain.users
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class InviteDomain(
    private val inviteEncoder: InviteEncoder,
    private val config: InviteDomainConfig,
) {
    val expireInviteTime = config.expireInviteTime
    val expiredState = config.expiredState
    val usedState = config.usedState
    val validState = config.validState
    val declinedState = config.declinedState

    fun generateInviteValue(): String =
        ByteArray(16).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            Base64.getUrlEncoder().encodeToString(byteArray)
        }

    fun createInviteValidationInformation(invite: String): InviteValidationInfo = inviteEncoder.createValidationInformation(invite)

    fun isInviteCodeValid(state: String): Boolean = state == validState

    fun isInviteTimeNotExpired(
        createdAt: Instant,
        clock: Clock,
    ): Boolean {
        val now = clock.now()
        return (now - createdAt) <= expireInviteTime
    }
}
