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
        return when (val result = lobbyService.createLobby(user.user, input.name, input.description, input.minPlayers, input.maxPlayers)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/lobbies/${result.value.id}")
                    .body(result.value.toOutputModel())

            is Either.Failure -> result.value.toProblemResponse()
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
        return when (val result = lobbyService.joinLobby(id, user.user)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())

            is Either.Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping("/api/lobbies/{id}/leave")
    fun leave(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = lobbyService.leaveLobby(id, user.user)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(mapOf("closed" to result.value))

            is Either.Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping("/api/lobbies/{id}")
    fun close(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = lobbyService.closeLobby(id, user.user)) {
            is Either.Success -> ResponseEntity.noContent().build<Unit>()
            is Either.Failure -> result.value.toProblemResponse()
        }
    }
}
