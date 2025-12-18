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

    // Global listeners (users in lobby list) - Key: UserId
    private val globalListeners = ConcurrentHashMap<Int, ListenerInfo>()

    // Lobby-specific listeners - Key: LobbyId -> List of Listeners
    private val lobbyListeners = ConcurrentHashMap<Int, CopyOnWriteArrayList<ListenerInfo>>()

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
    ): EventEmitter {
        logger.info("adding listener for userId={}, lobbyId={}", userId, lobbyId)
        val info = ListenerInfo(listener, userId, lobbyId)

        if (lobbyId == null) {
            val old = globalListeners.put(userId, info)
            if (old != null) {
                logger.info("removing stale global listener for userId={}", userId)
                try {
                    old.eventEmitter.complete()
                } catch (_: Exception) {
                }
            }
        } else {
            lobbyListeners.computeIfAbsent(lobbyId) { CopyOnWriteArrayList() }.add(info)
        }

        listener.onCompletion {
            removeListener(info)
        }
        listener.onError {
            removeListener(info)
        }
        return listener
    }

    fun notifyPlayerJoined(
        lobbyId: Int,
        userId: Int,
        playerName: String,
    ) {
        logger.info("notifying player joined: lobbyId={}, userId={}, playerName={}", lobbyId, userId, playerName)
        val event = Event.PlayerJoined(lobbyId, userId, playerName)
        sendEventToLobby(event, lobbyId)
    }

    fun notifyPlayerLeft(
        lobbyId: Int,
        playerId: Int,
        timestamp: String,
    ) {
        logger.info("notifying player left: lobbyId={}, playerId={}", lobbyId, playerId)
        val event = Event.PlayerLeft(lobbyId, playerId, timestamp)
        sendEventToLobby(event, lobbyId)
    }

    fun notifyNewLobby(
        lobbyId: Int,
        lobbyName: String,
    ) {
        logger.info("notifying new lobby: lobbyId={}, lobbyName={}", lobbyId, lobbyName)
        val event = Event.NewLobby(lobbyId, lobbyName)
        sendEventToGlobal(event)
    }

    fun notifyLobbyClosed(lobbyId: Int) {
        logger.info("notifying lobby closed: lobbyId={}", lobbyId)
        val event = Event.LobbyClosed(lobbyId)
        sendEventToGlobal(event)
    }

    fun notifyCountdownStarted(
        lobbyId: Int,
        expiresAt: Long,
    ) {
        logger.info("notifying countdown started: lobbyId={}, expiresAt={}", lobbyId, expiresAt)
        val event = Event.CountdownStarted(lobbyId, expiresAt)
        sendEventToLobby(event, lobbyId)
    }

    fun notifyLobbyUpdated(lobbyId: Int) {
        logger.info("notifying lobby updated: lobbyId={}", lobbyId)
        val event = Event.LobbyUpdated(lobbyId)
        sendEventToGlobal(event)
    }

    fun notifyGameCreated(
        lobbyId: Int,
        gameId: Int,
    ) {
        logger.info("notifying game created: lobbyId={}, gameId={}", lobbyId, gameId)
        val event = Event.GameStarted(lobbyId, gameId)
        sendEventToLobby(event, lobbyId)
    }

    fun getListener(userId: Int): EventEmitter? {
        return globalListeners[userId]?.eventEmitter
            ?: lobbyListeners.values.asSequence().flatten().find { it.userId == userId }?.eventEmitter
    }

    fun disconnectUser(listener: EventEmitter) {
        listener.complete()
    }

    private fun removeListener(info: ListenerInfo) {
        if (info.lobbyId == null) {
            if (globalListeners.remove(info.userId, info)) {
                logger.info("global listener removed for userId={}", info.userId)
            }
        } else {
            val list = lobbyListeners[info.lobbyId]
            if (list != null) {
                if (list.remove(info)) {
                    logger.info("lobby listener removed for userId={} lobbyId={}", info.userId, info.lobbyId)
                }
                if (list.isEmpty()) {
                    lobbyListeners.remove(info.lobbyId)
                }
            }
        }
    }

    private fun keepAlive() {
        val keepAliveEvent = Event.KeepAlive(Instant.now())
        globalListeners.values.forEach { info ->
            emitSafely(info, keepAliveEvent)
        }
        lobbyListeners.values.forEach { list ->
            list.forEach { info ->
                emitSafely(info, keepAliveEvent)
            }
        }
    }

    private fun sendEventToLobby(
        event: Event,
        lobbyId: Int,
    ) {
        logger.info("sending event to lobby {}: {}", lobbyId, event)
        lobbyListeners[lobbyId]?.forEach { info ->
            emitSafely(info, event)
        }
    }

    private fun sendEventToGlobal(event: Event) {
        globalListeners.values.forEach { info ->
            emitSafely(info, event)
        }
    }

    fun sendEventToUser(
        event: Event,
        userId: Int,
    ) {
        logger.info("sending event to userId={}: {}", userId, event)
        globalListeners[userId]?.let { emitSafely(it, event) }
        lobbyListeners.values.forEach { list ->
            list.filter { it.userId == userId }.forEach { emitSafely(it, event) }
        }
    }

    private fun emitSafely(
        info: ListenerInfo,
        event: Event,
    ) {
        try {
            info.eventEmitter.emit(event)
        } catch (ex: Exception) {
            logger.info("Exception while sending event to userId={} - {}", info.userId, ex.message)
        }
    }
}
