package pt.isel.domain.users
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
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
        expireInviteTime: Duration,
        now: Instant = Instant.now(),
    ): Boolean {
        return Duration.between(createdAt, now) <= expireInviteTime
    }
}
