package pt.isel.domain.users

import java.security.MessageDigest
import java.util.Base64

class Sha256InviteEncoder : InviteEncoder {
    override fun createValidationInformation(invite: String): InviteValidationInfo = InviteValidationInfo(hash(invite))

    private fun hash(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA256")
        return Base64.getUrlEncoder().encodeToString(
            messageDigest.digest(
                Charsets.UTF_8.encode(input).array(),
            ),
        )
    }
}
