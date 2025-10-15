package pt.isel.errors

sealed class GameError {
    data object InvalidNumberOfRounds : GameError()

    data object InvalidLobby : GameError()

    data object InvalidTime : GameError()

    data object GameNotFound : GameError()

    data object GameNotStarted : GameError()

    data object GameAlreadyEnded : GameError()
}
