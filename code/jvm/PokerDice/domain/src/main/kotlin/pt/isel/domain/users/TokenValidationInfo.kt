package pt.isel.domain.users

/**
 * Strongly typed information of token hashed by a TokenEncoder.
 */
data class TokenValidationInfo(
    val validationInfo: String,
)
