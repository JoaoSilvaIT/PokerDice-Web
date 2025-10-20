package pt.isel.domain.users

interface InviteEncoder {
    fun createValidationInformation(invite: String): InviteValidationInfo
}
