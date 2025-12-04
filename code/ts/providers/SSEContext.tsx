import React, { createContext, useContext, useCallback, useRef } from 'react';
import { RequestUri } from '../services/requestUri';

interface PlayerJoinedEvent {
    lobbyId: number;
    userId: number;
    playerName: string;
}

interface PlayerLeftEvent {
    lobbyId: number;
    playerId: number;
    timestamp: string;
}

interface LobbyCreatedEvent {
    lobbyId: number;
    lobbyName: string;
}

interface LobbyUpdatedEvent {
    lobbyId: number;
}

interface LobbyClosedEvent {
    lobbyId: number;
}

interface GameStartedEvent {
    lobbyId: number;
    gameId: number;
}

interface LobbyEventHandler {
    type: 'lobby';
    lobbyId: number;
    onPlayerJoined?: (event: PlayerJoinedEvent) => void;
    onPlayerLeft?: (event: PlayerLeftEvent) => void;
    onGameStarted?: (event: GameStartedEvent) => void;
}

interface AllLobbiesEventHandler {
    type: 'all-lobbies';
    onLobbyCreated?: (event: LobbyCreatedEvent) => void;
    onLobbyUpdated?: (event: LobbyUpdatedEvent) => void;
    onLobbyClosed?: (event: LobbyClosedEvent) => void;
}

type EventHandler = LobbyEventHandler | AllLobbiesEventHandler;

interface SSEContextType {
    connectToLobby: (lobbyId: number) => Promise<void>;
    connectToAllLobbies: () => Promise<void>;
    disconnect: () => void;
    isConnected: boolean;
    registerLobbyHandler: (
        lobbyId: number,
        onPlayerJoined?: (event: PlayerJoinedEvent) => void,
        onPlayerLeft?: (event: PlayerLeftEvent) => void,
        onGameStarted?: (event: GameStartedEvent) => void
    ) => void;
    registerAllLobbiesHandler: (
        onLobbyCreated?: (event: LobbyCreatedEvent) => void,
        onLobbyUpdated?: (event: LobbyUpdatedEvent) => void,
        onLobbyClosed?: (event: LobbyClosedEvent) => void
    ) => void;
    unregisterHandler: () => void;
}

const SSEContext = createContext<SSEContextType | undefined>(undefined);

export function SSEProvider({ children }: { children: React.ReactNode }) {
    const emitterRef = useRef<EventSource | null>(null);
    const [isConnected, setIsConnected] = React.useState(false);
    const handler = useRef<EventHandler | null>(null);

    const handlePlayerJoined = useCallback((event: MessageEvent) => {
        try {
            const data: PlayerJoinedEvent = JSON.parse(event.data);
            console.log('Player joined event:', data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'lobby' && currentHandler.lobbyId === data.lobbyId) {
                currentHandler.onPlayerJoined?.(data);
            }
        } catch (e) {
            console.error('Error parsing player-joined event:', e);
        }
    }, []);

    const handlePlayerLeft = useCallback((event: MessageEvent) => {
        try {
            const data: PlayerLeftEvent = JSON.parse(event.data);
            console.log('Player left event:', data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'lobby' && currentHandler.lobbyId === data.lobbyId) {
                currentHandler.onPlayerLeft?.(data);
            }
        } catch (e) {
            console.error('Error parsing player-left event:', e);
        }
    }, []);

    const handleLobbyCreated = useCallback((event: MessageEvent) => {
        try {
            const data: LobbyCreatedEvent = JSON.parse(event.data);
            console.log('Lobby created event:', data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'all-lobbies') {
                currentHandler.onLobbyCreated?.(data);
            }
        } catch (e) {
            console.error('Error parsing lobby-created event:', e);
        }
    }, []);

    const handleLobbyUpdated = useCallback((event: MessageEvent) => {
        try {
            const data: LobbyUpdatedEvent = JSON.parse(event.data);
            console.log('Lobby updated event:', data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'all-lobbies') {
                currentHandler.onLobbyUpdated?.(data);
            }
        } catch (e) {
            console.error('Error parsing lobby-updated event:', e);
        }
    }, []);

    const handleLobbyClosed = useCallback((event: MessageEvent) => {
        try {
            const data: LobbyClosedEvent = JSON.parse(event.data);
            console.log('Lobby closed event:', data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'all-lobbies') {
                currentHandler.onLobbyClosed?.(data);
            }
        } catch (e) {
            console.error('Error parsing lobby-closed event:', e);
        }
    }, []);

    const handleGameStarted = useCallback((event: MessageEvent) => {
        try {
            const data: GameStartedEvent = JSON.parse(event.data);
            console.log('Game started event:', data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'lobby' && currentHandler.lobbyId === data.lobbyId) {
                currentHandler.onGameStarted?.(data);
            }
        } catch (e) {
            console.error('Error parsing game-started event:', e);
        }
    }, []);

    const registerLobbyHandler = useCallback((
        lobbyId: number,
        onPlayerJoined?: (event: PlayerJoinedEvent) => void,
        onPlayerLeft?: (event: PlayerLeftEvent) => void,
        onGameStarted?: (event: GameStartedEvent) => void
    ) => {
        handler.current = {
            type: 'lobby',
            lobbyId,
            onPlayerJoined,
            onPlayerLeft,
            onGameStarted
        };
        console.log('Lobby handler registered for lobby:', lobbyId);
    }, []);

    const registerAllLobbiesHandler = useCallback((
        onLobbyCreated?: (event: LobbyCreatedEvent) => void,
        onLobbyUpdated?: (event: LobbyUpdatedEvent) => void,
        onLobbyClosed?: (event: LobbyClosedEvent) => void
    ) => {
        handler.current = {
            type: 'all-lobbies',
            onLobbyCreated,
            onLobbyUpdated,
            onLobbyClosed
        };
        console.log('All lobbies handler registered');
    }, []);

    const unregisterHandler = useCallback(() => {
        handler.current = null;
        console.log('Handler unregistered');
    }, []);

    const connectToLobby = useCallback((lobbyId: number) => {
        return new Promise<void>((resolve, reject) => {
            if (emitterRef.current) {
                console.log('Already connected to SSE');
                resolve();
                return;
            }

            console.log('Connecting to lobby SSE for lobby:', lobbyId);

            emitterRef.current = new EventSource(RequestUri.lobby.listen(lobbyId), {
                withCredentials: true
            });

            emitterRef.current.onopen = () => {
                console.log('SSE Connected to lobby:', lobbyId);
                setIsConnected(true);
                resolve();
            };

            emitterRef.current.onerror = (error) => {
                console.error('SSE Error:', error);
                setIsConnected(false);

                if (emitterRef.current) {
                    emitterRef.current.close();
                    emitterRef.current = null;
                }

                reject(error);
            };

            emitterRef.current.addEventListener('player-joined', handlePlayerJoined);
            emitterRef.current.addEventListener('player-left', handlePlayerLeft);
            emitterRef.current.addEventListener('game-started', handleGameStarted);
        });
    }, [handlePlayerJoined, handlePlayerLeft, handleGameStarted]);

    const connectToAllLobbies = useCallback(() => {
        return new Promise<void>((resolve, reject) => {
            if (emitterRef.current) {
                console.log('Already connected to SSE');
                resolve();
                return;
            }

            console.log('Connecting to all lobbies SSE');

            emitterRef.current = new EventSource(RequestUri.lobby.listenAll, {
                withCredentials: true
            });

            emitterRef.current.onopen = () => {
                console.log('SSE Connected to all lobbies');
                setIsConnected(true);
                resolve();
            };

            emitterRef.current.onerror = (error) => {
                console.error('SSE Error:', error);
                setIsConnected(false);

                if (emitterRef.current) {
                    emitterRef.current.close();
                    emitterRef.current = null;
                }

                reject(error);
            };

            emitterRef.current.addEventListener('new-lobby', handleLobbyCreated);
            emitterRef.current.addEventListener('lobby-updated', handleLobbyUpdated);
            emitterRef.current.addEventListener('lobby-closed', handleLobbyClosed);
        });
    }, [handleLobbyCreated, handleLobbyUpdated, handleLobbyClosed]);



    const disconnect = useCallback(() => {
        if (emitterRef.current) {
            console.log('Disconnecting from SSE');

            emitterRef.current.removeEventListener('player-joined', handlePlayerJoined);
            emitterRef.current.removeEventListener('player-left', handlePlayerLeft);
            emitterRef.current.removeEventListener('game-started', handleGameStarted);
            emitterRef.current.removeEventListener('new-lobby', handleLobbyCreated);
            emitterRef.current.removeEventListener('lobby-updated', handleLobbyUpdated);
            emitterRef.current.removeEventListener('lobby-closed', handleLobbyClosed);

            emitterRef.current.onopen = null;
            emitterRef.current.onerror = null;

            emitterRef.current.close();
            emitterRef.current = null;
            setIsConnected(false);
        }
    }, [handlePlayerJoined, handlePlayerLeft, handleGameStarted, handleLobbyCreated, handleLobbyUpdated, handleLobbyClosed]);

    return (
        <SSEContext.Provider value={{
            connectToLobby,
            connectToAllLobbies,
            disconnect,
            isConnected,
            registerLobbyHandler,
            registerAllLobbiesHandler,
            unregisterHandler
        }}>
            {children}
        </SSEContext.Provider>
    );
}

export function useSSE() {
    const context = useContext(SSEContext);
    if (context === undefined) {
        throw new Error('useSSE must be used within a SSEProvider');
    }
    return context;
}

