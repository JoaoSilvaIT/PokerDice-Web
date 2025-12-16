package pt.isel.model.game

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.GameEventService
import pt.isel.GameService
import pt.isel.domain.games.Dice
import pt.isel.domain.games.utils.charToFace
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.model.Problem
import pt.isel.model.sse.SseEmitterBasedEventEmitter
import pt.isel.utils.Either
import java.util.concurrent.TimeUnit

@RestController
class GameController(
    private val gameService: GameService,
    private val gameEventService: GameEventService,
) {
    @PostMapping("/api/games")
    fun create(
        user: AuthenticatedUser,
        @RequestBody input: GameCreateInputModel,
    ): ResponseEntity<*> {
        return when (val result = gameService.createGame(System.currentTimeMillis(), input.lobbyId, input.numberOfRounds, user.user.id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/games/${result.value.id}")
                    .body(result.value.toOutputModel())

            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @GetMapping("/api/games/{id}")
    fun getById(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val game =
            gameService.getGame(id)
                ?: return Problem.GameNotFound.response(HttpStatus.NOT_FOUND)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(game.toOutputModel())
    }

    @PostMapping("/api/games/{id}/start")
    fun start(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.startGame(id, user.user.id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/start")
    fun startRound(
        @PathVariable id: Int,
        @RequestBody input: SetAnteInputModel,
    ): ResponseEntity<*> {
        return when (val result = gameService.startNewRound(id, input.ante)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(result.value.toOutputModel())
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/ante")
    fun setAnte(
        user: AuthenticatedUser,
        @PathVariable id: Int,
        @RequestBody input: SetAnteInputModel,
    ): ResponseEntity<*> {
        val ante =
            input.ante ?: return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Ante is required"))

        return when (val result = gameService.setAnte(id, ante, user.user.id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/pay-ante")
    fun payAnte(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.payAnte(id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/next-turn")
    fun nextTurn(
        user: AuthenticatedUser,
        @PathVariable id: Int,
        @RequestBody input: SetAnteInputModel,
    ): ResponseEntity<*> {
        return when (val result = gameService.nextTurn(id, user.user.id, input.ante)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/update-turn")
    fun updateTurn(
        @RequestBody input: DiceUpdateInputModel,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val diceList = input.dices.map { Dice(charToFace(it.trim().first())) }
        return when (val result = gameService.updateTurn(diceList, id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(DiceOutputModel(result.value.currentRound!!.turn.currentDice))
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/roll-dices")
    fun rollDices(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.rollDices(id, user.user.id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(RolledDiceOutputModel(result.value))
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/distribute-winnings")
    fun distributeWinnings(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.distributeWinnings(id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value)
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/rounds/check-round-winner")
    fun checkRoundWinner(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.decideRoundWinner(id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.map { WinnersOutputModel(it.name, it.moneyWon) })
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/check-game-winner")
    fun checkGameWinner(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.decideGameWinner(id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.map { WinnersOutputModel(it.name, it.moneyWon) })
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @PostMapping("/api/games/{id}/end")
    fun end(
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        return when (val result = gameService.endGame(id, System.currentTimeMillis())) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(result.value.toOutputModel())
            is Either.Failure -> {
                result.value.toProblemResponse()
            }
        }
    }

    @GetMapping("/api/games/{id}/listen")
    fun listen(
        user: AuthenticatedUser,
        @PathVariable id: Int,
    ): ResponseEntity<SseEmitter> {
        val sseEmitter = SseEmitter(TimeUnit.HOURS.toMillis(1))
        gameEventService.addEventEmitter(
            SseEmitterBasedEventEmitter(sseEmitter),
            user.user.id,
            id,
        )
        return ResponseEntity
            .status(200)
            .header("Content-Type", "text/event-stream; charset=utf-8")
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .header("X-Accel-Buffering", "no")
            .body(sseEmitter)
    }
}
