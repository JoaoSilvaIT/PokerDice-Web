package pt.isel.utils

enum class Face {
    ACE,
    KING,
    QUEEN,
    JACK,
    TEN,
    NINE,
}

enum class State {
    RUNNING,
    TERMINATED,
    WAITING,
    FINISHED,
}

const val MAX_PLAYERS = 10
