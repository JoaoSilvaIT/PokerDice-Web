import React, { createContext, useContext, useCallback, useRef } from 'react';
import { RequestUri } from '../services/requestUri';

// Event types
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
    timestamp: string;
}


interface LobbyEventHandler {
    type: 'lobby';
    lobbyId: number;
    onPlayerJoined?: (event: PlayerJoinedEvent) => void;
    onPlayerLeft?: (event: PlayerLeftEvent) => void;
}

// Add more handler types as needed
// interface GameEventHandler { ... }

type EventHandler = LobbyEventHandler; // | GameEventHandler | ChatEventHandler

interface SSEContextType {
    connectToLobby: (lobbyId: number) => Promise<void>;
    disconnect: () => void;
    isConnected: boolean;
    registerLobbyHandler: (
        lobbyId: number,
        onPlayerJoined?: (event: PlayerJoinedEvent) => void,
        onPlayerLeft?: (event: PlayerLeftEvent) => void
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

    const registerLobbyHandler = useCallback((
        lobbyId: number,
        onPlayerJoined?: (event: PlayerJoinedEvent) => void,
        onPlayerLeft?: (event: PlayerLeftEvent) => void
    ) => {
        handler.current = {
            type: 'lobby',
            lobbyId,
            onPlayerJoined,
            onPlayerLeft
        };
        console.log('Lobby handler registered for lobby:', lobbyId);
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
        });
    }, [handlePlayerJoined, handlePlayerLeft]);

    const disconnect = useCallback(() => {
        if (emitterRef.current) {
            console.log('Disconnecting from SSE');

            emitterRef.current.removeEventListener('player-joined', handlePlayerJoined);
            emitterRef.current.removeEventListener('player-left', handlePlayerLeft);

            emitterRef.current.onopen = null;
            emitterRef.current.onerror = null;

            emitterRef.current.close();
            emitterRef.current = null;
            setIsConnected(false);
        }
    }, [handlePlayerJoined, handlePlayerLeft]);

    return (
        <SSEContext.Provider value={{
            connectToLobby,
            disconnect,
            isConnected,
            registerLobbyHandler,
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

