package pt.isel.errors

sealed class AuthTokenError {
    data object BlankEmail : AuthTokenError()
    data object BlankPassword : AuthTokenError()
    data object UserNotFoundOrInvalidCredentials : AuthTokenError()
}