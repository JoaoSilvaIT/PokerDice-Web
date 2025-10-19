package pt.isel.model.lobby

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.LobbyService
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.errors.LobbyError
import pt.isel.model.Problem
import pt.isel.utils.Either

@RestController
class LobbyController(
    private val lobbyService: LobbyService,
) {
    @PostMapping("/api/lobbies")
    fun create(
        user: AuthenticatedUser,
        @RequestBody input: LobbyCreateInputModel,
    ): ResponseEntity<*> {
        val result = lobbyService.createLobby(user.user, input.name, input.description, input.minPlayers, input.maxPlayers)
        return when (result) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/lobbies/${result.value.id}")
                    .body(
                        LobbyOutputModel(
                            id = result.value.id,
                            name = result.value.name,
                            description = result.value.description,
                            minPlayers = result.value.minPlayers,
                            maxPlayers = result.value.maxPlayers,
                            players =
                                result.value.users.map {
                                    LobbyPlayerOutputModel(it.id, it.name, it.email)
                                },
                            hostId = result.value.host.id,
                        ),
                    )
            is Either.Failure -> {
                when (result.value) {
                    LobbyError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MinPlayersTooLow -> Problem.MinPlayersTooLow.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MaxPlayersTooHigh -> Problem.MaxPlayersTooHigh.response(HttpStatus.BAD_REQUEST)
                    LobbyError.NameAlreadyUsed -> Problem.NameAlreadyUsed.response(HttpStatus.CONFLICT)
                    LobbyError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    LobbyError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.CONFLICT)
                    LobbyError.NotHost -> Problem.NotHost.response(HttpStatus.FORBIDDEN)
                    LobbyError.UserAlreadyInLobby -> Problem.UserAlreadyInLobby.response(HttpStatus.CONFLICT)
                    LobbyError.UserNotInLobby -> Problem.UserNotInLobby.response(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @GetMapping("/api/lobbies")
    fun list(): ResponseEntity<LobbyListOutputModel> {
        val lobbies =
            lobbyService.listVisibleLobbies().map { lobby ->
                LobbyOutputModel(
                    id = lobby.id,
                    name = lobby.name,
                    description = lobby.description,
                    minPlayers = lobby.minPlayers,
                    maxPlayers = lobby.maxPlayers,
                    players =
                        lobby.users.map {
                            LobbyPlayerOutputModel(it.id, it.name, it.email)
                        },
                    hostId = lobby.host.id,
                )
            }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(LobbyListOutputModel(lobbies))
    }

    @PostMapping("/api/lobbies/{id}/join")
    fun join(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = lobbyService.joinLobby(id, user.user)) {
            is Either.Success ->
                ResponseEntity.status(HttpStatus.OK).body(
                    LobbyOutputModel(
                        id = result.value.id,
                        name = result.value.name,
                        description = result.value.description,
                        minPlayers = result.value.minPlayers,
                        maxPlayers = result.value.maxPlayers,
                        players =
                            result.value.users.map {
                                LobbyPlayerOutputModel(it.id, it.name, it.email)
                            },
                        hostId = result.value.host.id,
                    ),
                )
            is Either.Failure -> {
                when (result.value) {
                    LobbyError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MinPlayersTooLow -> Problem.MinPlayersTooLow.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MaxPlayersTooHigh -> Problem.MaxPlayersTooHigh.response(HttpStatus.BAD_REQUEST)
                    LobbyError.NameAlreadyUsed -> Problem.NameAlreadyUsed.response(HttpStatus.CONFLICT)
                    LobbyError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    LobbyError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.CONFLICT)
                    LobbyError.NotHost -> Problem.NotHost.response(HttpStatus.FORBIDDEN)
                    LobbyError.UserAlreadyInLobby -> Problem.UserAlreadyInLobby.response(HttpStatus.CONFLICT)
                    LobbyError.UserNotInLobby -> Problem.UserNotInLobby.response(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @PostMapping("/api/lobbies/{id}/leave")
    fun leave(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = lobbyService.leaveLobby(id, user.user)) {
            is Either.Success -> ResponseEntity.status(HttpStatus.OK).body(mapOf("closed" to result.value))
            is Either.Failure -> {
                when (result.value) {
                    LobbyError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MinPlayersTooLow -> Problem.MinPlayersTooLow.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MaxPlayersTooHigh -> Problem.MaxPlayersTooHigh.response(HttpStatus.BAD_REQUEST)
                    LobbyError.NameAlreadyUsed -> Problem.NameAlreadyUsed.response(HttpStatus.CONFLICT)
                    LobbyError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    LobbyError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.CONFLICT)
                    LobbyError.NotHost -> Problem.NotHost.response(HttpStatus.FORBIDDEN)
                    LobbyError.UserAlreadyInLobby -> Problem.UserAlreadyInLobby.response(HttpStatus.CONFLICT)
                    LobbyError.UserNotInLobby -> Problem.UserNotInLobby.response(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @DeleteMapping("/api/lobbies/{id}")
    fun close(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = lobbyService.closeLobby(id, user.user)) {
            is Either.Success -> ResponseEntity.status(HttpStatus.NO_CONTENT).build<Unit>()
            is Either.Failure -> {
                when (result.value) {
                    LobbyError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MinPlayersTooLow -> Problem.MinPlayersTooLow.response(HttpStatus.BAD_REQUEST)
                    LobbyError.MaxPlayersTooHigh -> Problem.MaxPlayersTooHigh.response(HttpStatus.BAD_REQUEST)
                    LobbyError.NameAlreadyUsed -> Problem.NameAlreadyUsed.response(HttpStatus.CONFLICT)
                    LobbyError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    LobbyError.LobbyFull -> Problem.LobbyFull.response(HttpStatus.CONFLICT)
                    LobbyError.NotHost -> Problem.NotHost.response(HttpStatus.FORBIDDEN)
                    LobbyError.UserAlreadyInLobby -> Problem.UserAlreadyInLobby.response(HttpStatus.CONFLICT)
                    LobbyError.UserNotInLobby -> Problem.UserNotInLobby.response(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }
}
