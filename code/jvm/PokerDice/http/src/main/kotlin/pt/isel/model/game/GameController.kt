package pt.isel.model.game

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.GameService
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.errors.GameError
import pt.isel.utils.Either

@RestController
class GameController(
    private val gameService: GameService,
) {
    @PostMapping("/api/games")
    fun create(
        user: AuthenticatedUser,
        @RequestBody input: GameCreateInputModel,
    ): ResponseEntity<*> {
        return when (val result = gameService.createGame(System.currentTimeMillis(), input.lobbyId, input.numberOfRounds)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/games/${'$'}{result.value.gid}")
                    .body(GameOutputModel.fromDomain(result.value))

            is Either.Failure -> errorResponse(result.value)
        }
    }

    @GetMapping("/api/games/{id}")
    fun getById(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val game =
            gameService.getGame(id) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "GameNotFound"))

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GameOutputModel.fromDomain(game))
    }

    @PostMapping("/api/games/{id}/end")
    fun end(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.endGame(id, System.currentTimeMillis())) {
            is Either.Success -> ResponseEntity.status(HttpStatus.OK).body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> errorResponse(result.value)
        }
    }

    private fun errorResponse(error: GameError): ResponseEntity<*> {
        val status =
            when (error) {
                GameError.InvalidNumberOfRounds -> HttpStatus.BAD_REQUEST
                GameError.InvalidLobby -> HttpStatus.NOT_FOUND
                GameError.InvalidTime -> HttpStatus.BAD_REQUEST
                GameError.GameNotFound -> HttpStatus.NOT_FOUND
                GameError.GameNotStarted -> HttpStatus.CONFLICT
                GameError.GameAlreadyEnded -> HttpStatus.CONFLICT
                GameError.LobbyNotFound -> TODO()
                GameError.RoundNotFounded -> TODO()
                GameError.RoundNotStarted -> TODO()
            }
        return ResponseEntity
            .status(status)
            .body(mapOf("error" to (error::class.simpleName ?: "GameError")))
    }
}
