package pt.isel.model.sse

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.sse.Event
import pt.isel.domain.sse.EventEmitter

class SseEmitterBasedEventEmitter(
    private val sseEmitter: SseEmitter,
) : EventEmitter {
    override fun emit(event: Event) {
        val sseEvent =
            when (event) {
                is Event.PlayerJoined ->
                    SseEmitter
                        .event()
                        .name("player-joined")
                        .data(event)

                is Event.PlayerLeft ->
                    SseEmitter
                        .event()
                        .name("player-left")
                        .data(event)

                is Event.NewLobby ->
                    SseEmitter
                        .event()
                        .name("new-lobby")
                        .data(event)

                is Event.LobbyUpdated ->
                    SseEmitter
                        .event()
                        .name("lobby-updated")
                        .data(event)

                is Event.LobbyClosed ->
                    SseEmitter
                        .event()
                        .name("lobby-closed")
                        .data(event)

                is Event.GameStarted ->
                    SseEmitter
                        .event()
                        .name("game-started")
                        .data(event)

                is Event.TurnChanged ->
                    SseEmitter
                        .event()
                        .name("turn-changed")
                        .data(event)

                is Event.DiceRolled ->
                    SseEmitter
                        .event()
                        .name("dice-rolled")
                        .data(event)

                is Event.RoundUpdate ->
                    SseEmitter
                        .event()
                        .name("round-update")
                        .data(event)

                is Event.RoundEnded ->
                    SseEmitter
                        .event()
                        .name("round-ended")
                        .data(event)

                is Event.GameUpdated ->
                    SseEmitter
                        .event()
                        .name("game-updated")
                        .data(event)

                is Event.GameEnded ->
                    SseEmitter
                        .event()
                        .name("game-ended")
                        .data(event)

                is Event.KeepAlive ->
                    SseEmitter
                        .event()
                        .comment(event.timestamp.epochSecond.toString())
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
