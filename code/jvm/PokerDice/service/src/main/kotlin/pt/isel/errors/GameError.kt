package pt.isel.errors

import org.springframework.beans.factory.parsing.Problem
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

sealed class GameError {
    data object InvalidNumberOfRounds : GameError()

    data object InvalidLobby : GameError()

    data object LobbyNotFound : GameError()

    data object InvalidTime : GameError()

    data object GameNotFound : GameError()

    data object GameNotStarted : GameError()

    data object GameAlreadyEnded : GameError()

    data object RoundNotStarted : GameError()

    data object RoundNotFounded : GameError()

    data object FinalHandNotValid : GameError()

    data object GameNotFinished : GameError()

    data object LobbyHasActiveGame : GameError()

    data object HandAlreadyFull : GameError()
}
