package pt.isel.errors

sealed class LobbyError {
    data object BlankName : LobbyError()

    data object MinPlayersTooLow : LobbyError()

    data object MaxPlayersTooHigh : LobbyError()

    data object NameAlreadyUsed : LobbyError()

    data object LobbyNotFound : LobbyError()

    data object LobbyFull : LobbyError()

    data object NotHost : LobbyError()

    data object UserNotInLobby : LobbyError()

    data object UserAlreadyInLobby : LobbyError()

    data object GameAlreadyStarted : LobbyError()
}
