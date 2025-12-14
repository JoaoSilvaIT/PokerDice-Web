package pt.isel.timeout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class LobbyTimeoutManager(
    private val scope: CoroutineScope,
) {
    private data class Countdown(val job: Job, val expiresAt: Long)

    private val countdowns = ConcurrentHashMap<Int, Countdown>()

    @Volatile
    private var startHandler: (suspend (Int) -> Unit)? = null

    fun registerStartHandler(handler: suspend (Int) -> Unit) {
        startHandler = handler
    }

    fun startCountdown(
        lobbyId: Int,
        seconds: Long,
    ) {
        countdowns.computeIfAbsent(lobbyId) {
            val expiresAt = Instant.now().plusSeconds(seconds).toEpochMilli()
            val job =
                scope.launch {
                    try {
                        delay(seconds * 1000)
                        startHandler?.invoke(lobbyId)
                    } finally {
                        countdowns.remove(lobbyId)
                    }
                }
            Countdown(job, expiresAt)
        }
    }

    fun cancelCountdown(lobbyId: Int) {
        countdowns.remove(lobbyId)?.job?.cancel()
    }

    fun getExpiration(lobbyId: Int): Long? = countdowns[lobbyId]?.expiresAt
}
