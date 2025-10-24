package pt.isel.domain.users

interface TokenEncoder {
    fun createValidationInformation(token: String): TokenValidationInfo
}
