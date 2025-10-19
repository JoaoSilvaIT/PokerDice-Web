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
import pt.isel.model.Problem
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
                    .header("Location", "/api/games/${result.value.id}")
                    .body(GameOutputModel.fromDomain(result.value))

            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }

    @GetMapping("/api/games/{id}")
    fun getById(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val game = gameService.getGame(id)
            ?: return Problem.GameNotFound.response(HttpStatus.NOT_FOUND)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GameOutputModel.fromDomain(game))
    }

    @PostMapping("/api/games/{id}/start")
    fun start(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.startGame(id)) {
            is Either.Success -> ResponseEntity
                .status(HttpStatus.OK)
                .body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/start")
    fun startRound(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.startNewRound(id)) {
            is Either.Success -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/ante")
    fun setAnte(
        user: AuthenticatedUser,
        @PathVariable id: Int,
        @RequestBody input: SetAnteInputModel,
    ): ResponseEntity<*> {
        return when (val result = gameService.setAnte(id, input.ante)) {
            is Either.Success -> ResponseEntity
                .status(HttpStatus.OK)
                .body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/pay-ante")
    fun payAnte(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.payAnte(id)) {
            is Either.Success -> ResponseEntity
                .status(HttpStatus.OK)
                .body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/next-turn")
    fun nextTurn(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.nextTurn(id)) {
            is Either.Success -> ResponseEntity
                .status(HttpStatus.OK)
                .body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }

    @PostMapping("/api/games/{id}/end")
    fun end(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.endGame(id, System.currentTimeMillis())) {
            is Either.Success -> ResponseEntity.status(HttpStatus.OK).body(GameOutputModel.fromDomain(result.value))
            is Either.Failure -> {
                when (result.value) {
                    GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
                    GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
                    GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
                    GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
                    GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
                    GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
                    GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
                    GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
                }
            }
        }
    }
}
