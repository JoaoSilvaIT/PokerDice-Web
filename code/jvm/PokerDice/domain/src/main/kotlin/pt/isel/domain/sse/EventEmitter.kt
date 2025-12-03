package pt.isel.domain.sse

import pt.isel.domain.sse.Event

interface EventEmitter {
    fun emit(event: Event)

    fun onCompletion(callback: () -> Unit)

    fun onError(callback: (Throwable) -> Unit)

    fun complete()
}
