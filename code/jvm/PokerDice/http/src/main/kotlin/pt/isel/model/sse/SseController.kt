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
