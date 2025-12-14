package pt.isel.model.lobby

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.LobbyEventService
import pt.isel.LobbyService
import pt.isel.domain.sse.Event
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.model.sse.SseEmitterBasedEventEmitter
import pt.isel.timeout.LobbyTimeoutManager
import pt.isel.utils.Either
import java.util.concurrent.TimeUnit

@RestController
class LobbyController(
    private val lobbyService: LobbyService,
    private val lobbyEventService: LobbyEventService,
    private val lobbyTimeouts: LobbyTimeoutManager,
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

    @GetMapping("/api/lobbies/{id}")
    fun getLobby(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = lobbyService.getLobby(id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())

            is Either.Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping("/api/lobbies/{id}/listen")
    fun listen(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<SseEmitter> {
        val sseEmitter = SseEmitter(TimeUnit.HOURS.toMillis(1))
        lobbyEventService.addEventEmitter(
            SseEmitterBasedEventEmitter(sseEmitter),
            user.user.id,
            id,
        )

        lobbyTimeouts.getExpiration(id)?.let { expiresAt ->
            lobbyEventService.sendEventToUser(
                Event.CountdownStarted(id, expiresAt),
                user.user.id
            )
        }

        return ResponseEntity
            .status(200)
            .header("Content-Type", "text/event-stream; charset=utf-8")
            .header("Connection", "keep-alive")
            .header("X-Accel-Buffering", "no")
            .body(sseEmitter)
    }

    @GetMapping("/api/lobbies/listen")
    fun listenToAllLobbies(user: AuthenticatedUser): ResponseEntity<SseEmitter> {
        val sseEmitter = SseEmitter(TimeUnit.HOURS.toMillis(1))
        lobbyEventService.addEventEmitter(
            SseEmitterBasedEventEmitter(sseEmitter),
            user.user.id,
            null,
        )
        return ResponseEntity
            .status(200)
            .header("Content-Type", "text/event-stream; charset=utf-8")
            .header("Connection", "keep-alive")
            .header("X-Accel-Buffering", "no")
            .body(sseEmitter)
    }
}
