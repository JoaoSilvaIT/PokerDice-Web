package pt.isel

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pt.isel.domain.sse.Event
import pt.isel.domain.sse.EventEmitter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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

    // Map: GameId -> List of Listeners
    private val listenersMap = ConcurrentHashMap<Int, CopyOnWriteArrayList<GameListenerInfo>>()

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
    ): EventEmitter {
        logger.info("adding game listener for userId={}, gameId={}", userId, gameId)
        val info = GameListenerInfo(listener, userId, gameId)
        
        listenersMap.computeIfAbsent(gameId) { CopyOnWriteArrayList() }.add(info)

        listener.onCompletion {
            removeListener(info)
        }
        listener.onError {
            removeListener(info)
        }
        return listener
    }

    fun notifyTurnChanged(
        gameId: Int,
        turnUserId: Int,
        roundNumber: Int,
    ) {
        logger.info("notifying turn changed: gameId={}, turnUserId={}, roundNumber={}", gameId, turnUserId, roundNumber)
        val event = Event.TurnChanged(gameId, turnUserId, roundNumber)
        sendEventToGame(event, gameId)
    }

    fun notifyDiceRolled(
        gameId: Int,
        userId: Int,
        dice: List<String>,
    ) {
        logger.info("notifying dice rolled: gameId={}, userId={}, dice={}", gameId, userId, dice)
        val event = Event.DiceRolled(gameId, userId, dice)
        sendEventToGame(event, gameId)
    }

    fun notifyGameEnded(gameId: Int) {
        logger.info("notifying game ended: gameId={}", gameId)
        val event = Event.GameEnded(gameId)
        sendEventToGame(event, gameId)
    }

    fun notifyRoundEnded(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        logger.info("notifying round ended: gameId={}, roundNumber={}, winnerId={}", gameId, roundNumber, winnerId)
        val event = Event.RoundEnded(gameId, roundNumber, winnerId)
        sendEventToGame(event, gameId)
    }

    fun notifyGameUpdated(gameId: Int) {
        logger.info("notifying game updated: gameId={}", gameId)
        val event = Event.GameUpdated(gameId)
        sendEventToGame(event, gameId)
    }

    private fun removeListener(info: GameListenerInfo) {
        val list = listenersMap[info.gameId]
        if (list != null) {
            val removed = list.remove(info)
            if (removed) {
                logger.info("game listener removed")
            }
            if (list.isEmpty()) {
                listenersMap.remove(info.gameId)
            }
        }
    }

    private fun keepAlive() {
        val keepAliveEvent = Event.KeepAlive(Instant.now())
        listenersMap.values.forEach { list ->
            list.forEach { listenerInfo ->
                try {
                    listenerInfo.eventEmitter.emit(keepAliveEvent)
                } catch (ex: Exception) {
                    logger.info("Exception while sending keep-alive to userId={} - {}", listenerInfo.userId, ex.message)
                }
            }
        }
    }

    private fun sendEventToGame(
        event: Event,
        gameId: Int,
    ) {
        logger.info("sending event to game {}: {}", gameId, event)
        listenersMap[gameId]?.forEach { listenerInfo ->
            try {
                listenerInfo.eventEmitter.emit(event)
            } catch (ex: Exception) {
                logger.info("Exception while sending event to userId={} - {}", listenerInfo.userId, ex.message)
            }
        }
    }
}
