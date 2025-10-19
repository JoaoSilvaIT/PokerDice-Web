package pt.isel.model.game

import pt.isel.model.Problem
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.errors.GameError

fun GameError.toProblemResponse(): ResponseEntity<Any> = when(this) {
    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
    else -> Problem.FinalHandNotValid.response(HttpStatus.BAD_REQUEST)
}
