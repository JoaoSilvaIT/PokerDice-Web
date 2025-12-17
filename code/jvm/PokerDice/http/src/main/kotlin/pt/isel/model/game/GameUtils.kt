package pt.isel.model.game

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.domain.games.Game
import pt.isel.domain.games.utils.defineHandRank
import pt.isel.errors.GameError
import pt.isel.model.Problem

fun GameError.toProblemResponse(): ResponseEntity<Any> =
    when (this) {
        GameError.InvalidNumberOfRounds -> Problem.InvalidNumberOfRounds.response(HttpStatus.BAD_REQUEST)
        GameError.InvalidLobby -> Problem.InvalidLobby.response(HttpStatus.BAD_REQUEST)
        GameError.LobbyNotFound -> Problem.LobbyNotFound.response(HttpStatus.NOT_FOUND)
        GameError.InvalidTime -> Problem.InvalidTime.response(HttpStatus.BAD_REQUEST)
        GameError.GameNotFound -> Problem.GameNotFound.response(HttpStatus.NOT_FOUND)
        GameError.GameNotStarted -> Problem.GameNotStarted.response(HttpStatus.CONFLICT)
        GameError.GameAlreadyEnded -> Problem.GameAlreadyEnded.response(HttpStatus.CONFLICT)
        GameError.RoundNotStarted -> Problem.RoundNotStarted.response(HttpStatus.CONFLICT)
        GameError.RoundNotFounded -> Problem.RoundNotFound.response(HttpStatus.NOT_FOUND)
        GameError.FinalHandNotValid -> Problem.FinalHandNotValid.response(HttpStatus.BAD_REQUEST)
        GameError.GameNotFinished -> Problem.GameNotFinished.response(HttpStatus.CONFLICT)
        GameError.LobbyHasActiveGame -> Problem.LobbyHasActiveGame.response(HttpStatus.CONFLICT)
        GameError.HandAlreadyFull -> Problem.HandAlreadyFull.response(HttpStatus.BAD_REQUEST)
        GameError.InsufficientFunds -> Problem.InsufficientFunds.response(HttpStatus.BAD_REQUEST)
        GameError.NoRollsRemaining -> Problem.NoRollsRemaining.response(HttpStatus.BAD_REQUEST)
        GameError.UserNotLobbyHost -> Problem.UserNotLobbyHost.response(HttpStatus.FORBIDDEN)
        GameError.UserNotFirstPlayerOfRound -> Problem.UserNotFirstPlayerOfRound.response(HttpStatus.FORBIDDEN)
        GameError.UserNotPlayerOfTurn -> Problem.UserNotPlayerOfTurn.response(HttpStatus.FORBIDDEN)
        GameError.RoundWinnerNotDecided -> Problem.RoundWinnerNotDecided.response(HttpStatus.CONFLICT)
    }

fun Game.toOutputModel() =
    GameOutputModel(
        id = id,
        startedAt = startedAt,
        endedAt = endedAt,
        lobbyId = lobbyId,
        numberOfRounds = numberOfRounds,
        state = state.name,
        currentRound =
            currentRound?.let { round ->
                GameRoundOutputModel(
                    number = round.number,
                    ante = round.ante,
                    turnUserId = round.turn.player.id,
                    rollsLeft = round.turn.rollsRemaining,
                    currentDice = round.turn.currentDice.map { it.face.abbreviation },
                    pot = round.pot,
                    winners =
                        round.winners.map { player ->
                            PlayerInGameOutputModel(
                                id = player.id,
                                name = player.name,
                                currentBalance = player.currentBalance,
                                moneyWon = player.moneyWon,
                            )
                        }.takeIf { it.isNotEmpty() },
                    players = round.players.map { player ->
                        PlayerInGameOutputModel(
                            id = player.id,
                            name = player.name,
                            currentBalance = player.currentBalance,
                            moneyWon = player.moneyWon,
                        )
                    }
                )
            },
        players =
            players.map { player ->
                // Find hand by player ID since PlayerInGame equality includes all fields
                // which may differ between game.players and round.playerHands keys
                val handRank =
                    currentRound?.playerHands?.entries?.find { it.key.id == player.id }?.value?.let { hand ->
                        if (hand.dices.size == 5) {
                            defineHandRank(hand).second.name.replace("_", " ").lowercase()
                                .split(' ')
                                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        } else {
                            null
                        }
                    }

                PlayerInGameOutputModel(
                    id = player.id,
                    name = player.name,
                    currentBalance = player.currentBalance,
                    moneyWon = player.moneyWon,
                    handRank = handRank,
                )
            },
    )
