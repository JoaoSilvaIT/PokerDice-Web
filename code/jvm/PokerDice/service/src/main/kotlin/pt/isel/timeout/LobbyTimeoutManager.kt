package pt.isel.timeout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class LobbyTimeoutManager(
    private val scope: CoroutineScope
) {
    private val jobs = ConcurrentHashMap<Int, Job>()
    @Volatile
    private var startHandler: (suspend (Int) -> Unit)? = null

    fun registerStartHandler(handler: suspend (Int) -> Unit) {
        startHandler = handler
    }

    fun startCountdown(lobbyId: Int, seconds: Long) {
        jobs.computeIfAbsent(lobbyId) {
            scope.launch {
                try {
                    delay(seconds * 1000)
                    startHandler?.invoke(lobbyId)
                } finally {
                    jobs.remove(lobbyId)
                }
            }
        }
    }

    fun cancelCountdown(lobbyId: Int) {
        jobs.remove(lobbyId)?.cancel()
    }
}
