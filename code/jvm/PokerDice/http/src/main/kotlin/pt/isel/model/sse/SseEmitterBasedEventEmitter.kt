package pt.isel.model.sse

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.sse.Event
import pt.isel.domain.sse.EventEmitter

class SseEmitterBasedEventEmitter(
    private val sseEmitter: SseEmitter,
) : EventEmitter {
    override fun emit(event: Event) {
        val sseEvent =
            if (event is Event.KeepAlive) {
                SseEmitter
                    .event()
                    .comment(event.timestamp.epochSecond.toString())
            } else {
                val eventName =
                    when (event) {
                        is Event.PlayerJoined -> "player-joined"
                        is Event.PlayerLeft -> "player-left"
                        is Event.NewLobby -> "new-lobby"
                        is Event.LobbyUpdated -> "lobby-updated"
                        is Event.LobbyClosed -> "lobby-closed"
                        is Event.GameStarted -> "game-started"
                        is Event.CountdownStarted -> "countdown-started"
                        is Event.TurnChanged -> "turn-changed"
                        is Event.DiceRolled -> "dice-rolled"
                        is Event.RoundUpdate -> "round-update"
                        is Event.RoundEnded -> "round-ended"
                        is Event.GameUpdated -> "game-updated"
                        is Event.GameEnded -> "game-ended"
                        is Event.KeepAlive -> throw IllegalStateException("KeepAlive handled separately")
                    }
                SseEmitter.event().name(eventName).data(event)
            }
        sseEmitter.send(sseEvent)
    }

    override fun onCompletion(callback: () -> Unit) {
        sseEmitter.onCompletion(callback)
    }

    override fun onError(callback: (Throwable) -> Unit) {
        sseEmitter.onError(callback)
    }

    override fun complete() {
        sseEmitter.complete()
    }
}
