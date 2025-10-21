package pt.isel.model

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH =
    "https://github.com/isel-leic-daw/2025-daw-leic51d-06/tree/main/docs/problems/"

sealed class Problem(
    typeUri: URI,
) {
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    // Authentication errors
    data object BlankEmail : Problem(URI("$PROBLEM_URI_PATH/blank-email"))

    data object BlankPassword : Problem(URI("$PROBLEM_URI_PATH/blank-password"))

    data object BlankInvite : Problem(URI("$PROBLEM_URI_PATH/blank-invite"))

    data object UserNotFoundOrInvalidCredentials : Problem(URI("$PROBLEM_URI_PATH/user-not-found-or-invalid-credentials"))

    data object EmailAlreadyInUse : Problem(URI("$PROBLEM_URI_PATH/email-already-in-use"))

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object InvalidOrMissingToken : Problem(URI("$PROBLEM_URI_PATH/invalid-or-missing-token"))

    data object InvalidInvite : Problem(URI("$PROBLEM_URI_PATH/invalid-invite"))

    // Lobby errors
    data object BlankName : Problem(URI("$PROBLEM_URI_PATH/blank-name"))

    data object MinPlayersTooLow : Problem(URI("$PROBLEM_URI_PATH/min-players-too-low"))

    data object MaxPlayersTooHigh : Problem(URI("$PROBLEM_URI_PATH/max-players-too-high"))

    data object NameAlreadyUsed : Problem(URI("$PROBLEM_URI_PATH/name-already-used"))

    data object LobbyNotFound : Problem(URI("$PROBLEM_URI_PATH/lobby-not-found"))

    data object LobbyFull : Problem(URI("$PROBLEM_URI_PATH/lobby-full"))

    data object NotHost : Problem(URI("$PROBLEM_URI_PATH/not-host"))

    data object UserNotInLobby : Problem(URI("$PROBLEM_URI_PATH/user-not-in-lobby"))

    data object UserAlreadyInLobby : Problem(URI("$PROBLEM_URI_PATH/user-already-in-lobby"))

    // Game errors
    data object InvalidNumberOfRounds : Problem(URI("$PROBLEM_URI_PATH/invalid-number-of-rounds"))

    data object InvalidLobby : Problem(URI("$PROBLEM_URI_PATH/invalid-lobby"))

    data object InvalidTime : Problem(URI("$PROBLEM_URI_PATH/invalid-time"))

    data object GameNotFound : Problem(URI("$PROBLEM_URI_PATH/game-not-found"))

    data object GameNotStarted : Problem(URI("$PROBLEM_URI_PATH/game-not-started"))

    data object GameAlreadyEnded : Problem(URI("$PROBLEM_URI_PATH/game-already-ended"))

    data object RoundNotStarted : Problem(URI("$PROBLEM_URI_PATH/round-not-started"))

    data object RoundNotFound : Problem(URI("$PROBLEM_URI_PATH/round-not-found"))

    data object FinalHandNotValid : Problem(URI("$PROBLEM_URI_PATH/final-hand-not-valid"))
}
