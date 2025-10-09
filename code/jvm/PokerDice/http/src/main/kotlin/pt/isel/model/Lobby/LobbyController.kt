package pt.isel.model.Lobby

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.LobbyService
import pt.isel.domain.AuthenticatedUser
import pt.isel.errors.LobbyError
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
        val result = lobbyService.createLobby(user.user, input.name, input.description, input.minPlayers)
        return when (result) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/lobbies/${result.value.id}")
                    .body(result.value.toOutputModel())
            is Either.Failure -> errorResponse(result.value)
        }
    }

    @GetMapping("/api/lobbies")
    fun list(): ResponseEntity<LobbyListOutputModel> {
        val lobbies = lobbyService.listVisibleLobbies().map { it.toOutputModel() }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(LobbyListOutputModel(lobbies))
    }

    @PostMapping("/api/lobbies/{id}/join")
    fun join(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result = lobbyService.joinLobby(id, user.user)
        return when (result) {
            is Either.Success -> ResponseEntity.status(HttpStatus.OK).body(result.value.toOutputModel())
            is Either.Failure -> errorResponse(result.value)
        }
    }

    @PostMapping("/api/lobbies/{id}/leave")
    fun leave(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result = lobbyService.leaveLobby(id, user.user)
        return when (result) {
            is Either.Success -> ResponseEntity.status(HttpStatus.OK).body(mapOf("closed" to result.value))
            is Either.Failure -> errorResponse(result.value)
        }
    }

    @DeleteMapping("/api/lobbies/{id}")
    fun close(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val result = lobbyService.closeLobby(id, user.user)
        return when (result) {
            is Either.Success -> ResponseEntity.status(HttpStatus.NO_CONTENT).build<Unit>()
            is Either.Failure -> errorResponse(result.value)
        }
    }

    private fun errorResponse(error: LobbyError): ResponseEntity<*> {
        val status =
            when (error) {
                LobbyError.BlankName, LobbyError.MinPlayersTooLow -> HttpStatus.BAD_REQUEST
                LobbyError.NameAlreadyUsed, LobbyError.LobbyFull -> HttpStatus.CONFLICT
                LobbyError.LobbyNotFound -> HttpStatus.NOT_FOUND
                LobbyError.NotHost -> HttpStatus.FORBIDDEN
            }
        return ResponseEntity
            .status(status)
            .body(mapOf("error" to (error::class.simpleName ?: "LobbyError")))
    }
}
