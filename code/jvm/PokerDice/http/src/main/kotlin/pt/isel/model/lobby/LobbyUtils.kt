package pt.isel.model.lobby

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.domain.lobby.Lobby
import pt.isel.errors.LobbyError
import pt.isel.model.Problem

fun LobbyError.toProblemResponse(): ResponseEntity<Any> =
    when (this) {
        LobbyError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
        LobbyError.MinPlayersTooLow -> Problem.MinPlayersTooLow.response(HttpStatus.BAD_REQUEST)
        LobbyError.MaxPlayersTooHigh -> Problem.MaxPlayersTooHigh.response(HttpStatus.BAD_REQUEST)
        LobbyError.NameAlreadyUsed -> Problem.NameAlreadyUsed.response(HttpStatus.CONFLICT)
        LobbyError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
        LobbyError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.CONFLICT)
        LobbyError.NotHost -> Problem.NotHost.response(HttpStatus.FORBIDDEN)
        LobbyError.UserAlreadyInLobby -> Problem.UserAlreadyInLobby.response(HttpStatus.CONFLICT)
        LobbyError.UserNotInLobby -> Problem.UserNotInLobby.response(HttpStatus.BAD_REQUEST)
        LobbyError.GameAlreadyStarted -> Problem.GameAlreadyStarted.response(HttpStatus.CONFLICT)
    }

fun Lobby.toOutputModel() =
    LobbyOutputModel(
        id = id,
        name = name,
        description = description,
        minPlayers = settings.minPlayers,
        maxPlayers = settings.maxPlayers,
        players = players.map { LobbyPlayerOutputModel(it.id, it.name) },
        hostId = host.id,
    )
