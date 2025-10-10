package pt.isel.errors

sealed class GameError {
    data object InvalidNumberOfRounds : GameError()

    data object InvalidLobby : GameError()

    data object InvalidStartTime : GameError()
}
