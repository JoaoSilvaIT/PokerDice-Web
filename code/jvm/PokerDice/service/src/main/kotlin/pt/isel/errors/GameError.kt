package pt.isel.errors

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
