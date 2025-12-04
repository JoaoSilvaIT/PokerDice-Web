package pt.isel

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pt.isel.domain.sse.Event
import pt.isel.domain.sse.EventEmitter
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class GameListenerInfo(
    val eventEmitter: EventEmitter,
    val userId: Int,
    val gameId: Int,
)

@Component
class GameEventService {
    companion object {
        private val logger = LoggerFactory.getLogger(GameEventService::class.java)
    }

    private val listeners = mutableListOf<GameListenerInfo>()
    private val lock = ReentrantLock()

    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1).also {
            it.scheduleAtFixedRate({ keepAlive() }, 2, 2, TimeUnit.SECONDS)
        }

    @PreDestroy
    fun shutdown() {
        logger.info("shutting down GameEventService")
        scheduler.shutdown()
    }

    fun addEventEmitter(
        listener: EventEmitter,
        userId: Int,
        gameId: Int,
    ): EventEmitter =
        lock.withLock {
            logger.info("adding game listener for userId={}, gameId={}", userId, gameId)
            listeners.add(GameListenerInfo(listener, userId, gameId))
            listener.onCompletion {
                removeListener(listener)
            }
            listener.onError {
                removeListener(listener)
            }
            listener
        }

    fun notifyTurnChanged(
        gameId: Int,
        turnUserId: Int,
        roundNumber: Int,
    ) = lock.withLock {
        logger.info("notifying turn changed: gameId={}, turnUserId={}, roundNumber={}", gameId, turnUserId, roundNumber)
        val event = Event.TurnChanged(gameId, turnUserId, roundNumber)
        sendEventToGame(event, gameId)
    }

    fun notifyDiceRolled(
        gameId: Int,
        userId: Int,
        dice: List<String>,
    ) = lock.withLock {
        logger.info("notifying dice rolled: gameId={}, userId={}, dice={}", gameId, userId, dice)
        val event = Event.DiceRolled(gameId, userId, dice)
        sendEventToGame(event, gameId)
    }

    fun notifyGameEnded(
        gameId: Int,
    ) = lock.withLock {
        logger.info("notifying game ended: gameId={}", gameId)
        val event = Event.GameEnded(gameId)
        sendEventToGame(event, gameId)
    }

    fun notifyRoundEnded(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) = lock.withLock {
        logger.info("notifying round ended: gameId={}, roundNumber={}, winnerId={}", gameId, roundNumber, winnerId)
        val event = Event.RoundEnded(gameId, roundNumber, winnerId)
        sendEventToGame(event, gameId)
    }

    fun notifyGameUpdated(
        gameId: Int,
    ) = lock.withLock {
        logger.info("notifying game updated: gameId={}", gameId)
        val event = Event.GameUpdated(gameId)
        sendEventToGame(event, gameId)
    }

    private fun removeListener(listener: EventEmitter) =
        lock.withLock {
            val removed = listeners.removeIf { it.eventEmitter == listener }
            if (removed) {
                logger.info("game listener removed")
            }
        }

    private fun keepAlive() =
        lock.withLock {
            val keepAliveEvent = Event.KeepAlive(Instant.now())
            listeners.forEach { listenerInfo ->
                try {
                    listenerInfo.eventEmitter.emit(keepAliveEvent)
                } catch (ex: Exception) {
                    logger.info("Exception while sending keep-alive to userId={} - {}", listenerInfo.userId, ex.message)
                }
            }
        }

    private fun sendEventToGame(
        event: Event,
        gameId: Int,
    ) {
        logger.info("sending event to game {}: {}", gameId, event)
        listeners
            .filter { it.gameId == gameId }
            .forEach { listenerInfo ->
                try {
                    listenerInfo.eventEmitter.emit(event)
                } catch (ex: Exception) {
                    logger.info("Exception while sending event to userId={} - {}", listenerInfo.userId, ex.message)
                }
            }
    }
}

