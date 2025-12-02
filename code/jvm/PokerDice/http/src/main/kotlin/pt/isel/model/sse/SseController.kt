package pt.isel.model.sse

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.sse.Event

class SseEmitterBasedEventEmitter(
    private val sseEmitter: SseEmitter,
) : EventEmitter {
    override fun emit(event: Event) {
        val event =
            when (event) {
                is Event.Message ->
                    SseEmitter
                        .event()
                        .id(event.id.toString())
                        .name("message")
                        .data(event)

                is Event.KeepAlive ->
                    SseEmitter
                        .event()
                        .comment(event.timestamp.epochSecond.toString())
            }
        sseEmitter.send(event)
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
