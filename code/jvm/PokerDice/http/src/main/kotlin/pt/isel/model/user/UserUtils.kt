package pt.isel.model.user

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.errors.AuthTokenError
import pt.isel.model.Problem

fun AuthTokenError.toProblemResponse(): ResponseEntity<Any> =
    when (this) {
        AuthTokenError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
        AuthTokenError.BlankEmail -> Problem.BlankEmail.response(HttpStatus.BAD_REQUEST)
        AuthTokenError.BlankPassword -> Problem.BlankPassword.response(HttpStatus.BAD_REQUEST)
        AuthTokenError.EmailAlreadyInUse -> Problem.EmailAlreadyInUse.response(HttpStatus.CONFLICT)
        AuthTokenError.UserNotFoundOrInvalidCredentials ->
            Problem.UserNotFoundOrInvalidCredentials.response(HttpStatus.UNAUTHORIZED)
        AuthTokenError.BlankInvite -> Problem.BlankInvite.response(HttpStatus.BAD_REQUEST)
        AuthTokenError.InvalidInvite -> Problem.InvalidInvite.response(HttpStatus.BAD_REQUEST)
    }
