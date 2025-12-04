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

data class ListenerInfo(
    val eventEmitter: EventEmitter,
    val userId: Int,
    val lobbyId: Int? = null,
)

@Component
class LobbyEventService {
    companion object {
        private val logger = LoggerFactory.getLogger(LobbyEventService::class.java)
    }

    // Important: mutable state on a singleton service
    private val listeners = mutableListOf<ListenerInfo>()
    private val lock = ReentrantLock()

    // A scheduler to send the periodic keep-alive events
    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1).also {
            it.scheduleAtFixedRate({ keepAlive() }, 2, 2, TimeUnit.SECONDS)
        }

    @PreDestroy
    fun shutdown() {
        logger.info("shutting down LobbyEventService")
        scheduler.shutdown()
    }

    fun addEventEmitter(
        listener: EventEmitter,
        userId: Int,
        lobbyId: Int? = null,
    ): EventEmitter =
        lock.withLock {
            logger.info("adding listener for userId={}, lobbyId={}", userId, lobbyId)
            listeners.add(ListenerInfo(listener, userId, lobbyId))
            listener.onCompletion {
                removeListener(listener)
            }
            listener.onError {
                removeListener(listener)
            }
            listener
        }

    fun notifyPlayerJoined(
        lobbyId: Int,
        userId: Int,
        playerName: String,
    ) = lock.withLock {
        logger.info("notifying player joined: lobbyId={}, userId={}, playerName={}", lobbyId, userId, playerName)
        val event = Event.PlayerJoined(lobbyId, userId, playerName)
        sendEventToLobby(event, lobbyId)
    }

    fun notifyPlayerLeft(
        lobbyId: Int,
        playerId: Int,
        timestamp: String,
    ) = lock.withLock {
        logger.info("notifying player left: lobbyId={}, playerId={}", lobbyId, playerId)
        val event = Event.PlayerLeft(lobbyId, playerId, timestamp)
        sendEventToLobby(event, lobbyId)
    }

    fun notifyNewLobby(
        lobbyId: Int,
        lobbyName: String,
    ) = lock.withLock {
        logger.info("notifying new lobby: lobbyId={}, lobbyName={}", lobbyId, lobbyName)
        val event = Event.NewLobby(lobbyId, lobbyName)
        sendEventToAll(event)
    }

    fun notifyLobbyClosed(lobbyId: Int) =
        lock.withLock {
            logger.info("notifying lobby closed: lobbyId={}", lobbyId)
            val event = Event.LobbyClosed(lobbyId)
            sendEventToAll(event)
        }

    fun notifyLobbyUpdated(lobbyId: Int) =
        lock.withLock {
            logger.info("notifying lobby updated: lobbyId={}", lobbyId)
            val event = Event.LobbyUpdated(lobbyId)
            sendEventToAll(event)
        }

    fun getListener(userId: Int): EventEmitter? =
        lock.withLock {
            listeners.find { it.userId == userId }?.eventEmitter
        }

    fun disconnectUser(listener: EventEmitter) =
        lock.withLock {
            listener.complete()
        }

    private fun removeListener(listener: EventEmitter) =
        lock.withLock {
            val removed = listeners.removeIf { it.eventEmitter == listener }
            if (removed) {
                logger.info("listener removed")
            }
        }

    private fun keepAlive() =
        lock.withLock {
            val keepAliveEvent = Event.KeepAlive(Instant.now())
            sendEventToAll(keepAliveEvent)
        }

    private fun sendEventToLobby(
        event: Event,
        lobbyId: Int,
    ) {
        logger.info("sending event to lobby {}: {}", lobbyId, event)
        listeners
            .filter { it.lobbyId == lobbyId }
            .forEach { listenerInfo ->
                try {
                    listenerInfo.eventEmitter.emit(event)
                } catch (ex: Exception) {
                    logger.info("Exception while sending event to userId={} - {}", listenerInfo.userId, ex.message)
                }
            }
    }

    private fun sendEventToAll(event: Event) {
        listeners.forEach { listenerInfo ->
            try {
                listenerInfo.eventEmitter.emit(event)
            } catch (ex: Exception) {
                logger.info("Exception while sending keep-alive to userId={} - {}", listenerInfo.userId, ex.message)
            }
        }
    }

    fun sendEventToUser(
        event: Event,
        userId: Int,
    ) = lock.withLock {
        logger.info("sending event to userId={}: {}", userId, event)
        listeners
            .filter { it.userId == userId }
            .forEach { listenerInfo ->
                try {
                    listenerInfo.eventEmitter.emit(event)
                } catch (ex: Exception) {
                    logger.info("Exception while sending event to userId={} - {}", userId, ex.message)
                }
            }
    }
}
