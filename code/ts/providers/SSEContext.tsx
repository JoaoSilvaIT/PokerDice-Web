import React, {createContext, useCallback, useContext, useRef} from 'react';
import {RequestUri} from '../services/requestUri';

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

interface TurnChangedEvent {
    gameId: number;
    turnUserId: number;
    roundNumber: number;
}

interface DiceRolledEvent {
    gameId: number;
    userId: number;
    dice: string[];
}

interface RoundEndedEvent {
    gameId: number;
    roundNumber: number;
    winnerId: number;
}

interface GameUpdatedEvent {
    gameId: number;
}

interface GameEndedEvent {
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

interface GameEventHandler {
    type: 'game';
    gameId: number;
    onTurnChanged?: (event: TurnChangedEvent) => void;
    onDiceRolled?: (event: DiceRolledEvent) => void;
    onRoundEnded?: (event: RoundEndedEvent) => void;
    onGameUpdated?: (event: GameUpdatedEvent) => void;
    onGameEnded?: (event: GameEndedEvent) => void;
}

type EventHandler = LobbyEventHandler | AllLobbiesEventHandler | GameEventHandler;

interface SSEContextType {
    connectToLobby: (lobbyId: number) => Promise<void>;
    connectToAllLobbies: () => Promise<void>;
    connectToGame: (gameId: number) => Promise<void>;
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
    registerGameHandler: (
        gameId: number,
        onTurnChanged?: (event: TurnChangedEvent) => void,
        onDiceRolled?: (event: DiceRolledEvent) => void,
        onRoundEnded?: (event: RoundEndedEvent) => void,
        onGameUpdated?: (event: GameUpdatedEvent) => void,
        onGameEnded?: (event: GameEndedEvent) => void
    ) => void;
    unregisterHandler: () => void;
}

const SSEContext = createContext<SSEContextType | undefined>(undefined);

export function SSEProvider({children}: { children: React.ReactNode }) {
    const emitterRef = useRef<EventSource | null>(null);
    const [isConnected, setIsConnected] = React.useState(false);
    const handler = useRef<EventHandler | null>(null);

    const handlePlayerJoined = useCallback((event: MessageEvent) => {
        try {
            const data: PlayerJoinedEvent = JSON.parse(event.data);

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
            const currentHandler = handler.current;
            if (currentHandler?.type === 'lobby' && currentHandler.lobbyId === data.lobbyId) {
                currentHandler.onGameStarted?.(data);
            }
        } catch (e) {
            console.error('Error parsing game-started event:', e);
        }
    }, []);

    const handleTurnChanged = useCallback((event: MessageEvent) => {
        try {
            const data: TurnChangedEvent = JSON.parse(event.data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'game' && currentHandler.gameId === data.gameId) {
                currentHandler.onTurnChanged?.(data);
            }
        } catch (e) {
            console.error('Error parsing turn-changed event:', e);
        }
    }, []);

    const handleDiceRolled = useCallback((event: MessageEvent) => {
        try {
            const data: DiceRolledEvent = JSON.parse(event.data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'game' && currentHandler.gameId === data.gameId) {
                currentHandler.onDiceRolled?.(data);
            }
        } catch (e) {
            console.error('Error parsing dice-rolled event:', e);
        }
    }, []);

    const handleRoundEnded = useCallback((event: MessageEvent) => {
        try {
            const data: RoundEndedEvent = JSON.parse(event.data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'game' && currentHandler.gameId === data.gameId) {
                currentHandler.onRoundEnded?.(data);
            }
        } catch (e) {
            console.error('Error parsing round-ended event:', e);
        }
    }, []);

    const handleGameUpdated = useCallback((event: MessageEvent) => {
        try {
            const data: GameUpdatedEvent = JSON.parse(event.data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'game' && currentHandler.gameId === data.gameId) {
                currentHandler.onGameUpdated?.(data);
            }
        } catch (e) {
            console.error('Error parsing game-updated event:', e);
        }
    }, []);

    const handleGameEnded = useCallback((event: MessageEvent) => {
        try {
            const data: GameEndedEvent = JSON.parse(event.data);

            const currentHandler = handler.current;
            if (currentHandler?.type === 'game' && currentHandler.gameId === data.gameId) {
                currentHandler.onGameEnded?.(data);
            }
        } catch (e) {
            console.error('Error parsing game-ended event:', e);
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
    }, []);

    const registerGameHandler = useCallback((
        gameId: number,
        onTurnChanged?: (event: TurnChangedEvent) => void,
        onDiceRolled?: (event: DiceRolledEvent) => void,
        onRoundEnded?: (event: RoundEndedEvent) => void,
        onGameUpdated?: (event: GameUpdatedEvent) => void,
        onGameEnded?: (event: GameEndedEvent) => void
    ) => {
        handler.current = {
            type: 'game',
            gameId,
            onTurnChanged,
            onDiceRolled,
            onRoundEnded,
            onGameUpdated,
            onGameEnded
        };
    }, []);

    const unregisterHandler = useCallback(() => {
        handler.current = null;
    }, []);

    const connectToLobby = useCallback((lobbyId: number) => {
        return new Promise<void>((resolve, reject) => {
            if (emitterRef.current) {
                resolve();
                return;
            }


            emitterRef.current = new EventSource(RequestUri.lobby.listen(lobbyId), {
                withCredentials: true
            });

            emitterRef.current.onopen = () => {
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
                resolve();
                return;
            }

            emitterRef.current = new EventSource(RequestUri.lobby.listenAll, {
                withCredentials: true
            });

            emitterRef.current.onopen = () => {
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

    const connectToGame = useCallback((gameId: number) => {
        return new Promise<void>((resolve, reject) => {
            // Force disconnect any existing connection before connecting to game
            if (emitterRef.current) {
                emitterRef.current.close();
                emitterRef.current = null;
                setIsConnected(false);
            }


            emitterRef.current = new EventSource(`/api/games/${gameId}/listen`, {
                withCredentials: true
            });

            emitterRef.current.onopen = () => {
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

            emitterRef.current.addEventListener('turn-changed', handleTurnChanged);
            emitterRef.current.addEventListener('dice-rolled', handleDiceRolled);
            emitterRef.current.addEventListener('round-ended', handleRoundEnded);
            emitterRef.current.addEventListener('game-updated', handleGameUpdated);
            emitterRef.current.addEventListener('game-ended', handleGameEnded);
        });
    }, [handleTurnChanged, handleDiceRolled, handleRoundEnded, handleGameUpdated, handleGameEnded]);


    const disconnect = useCallback(() => {
        if (emitterRef.current) {

            emitterRef.current.removeEventListener('player-joined', handlePlayerJoined);
            emitterRef.current.removeEventListener('player-left', handlePlayerLeft);
            emitterRef.current.removeEventListener('game-started', handleGameStarted);
            emitterRef.current.removeEventListener('new-lobby', handleLobbyCreated);
            emitterRef.current.removeEventListener('lobby-updated', handleLobbyUpdated);
            emitterRef.current.removeEventListener('lobby-closed', handleLobbyClosed);
            emitterRef.current.removeEventListener('turn-changed', handleTurnChanged);
            emitterRef.current.removeEventListener('dice-rolled', handleDiceRolled);
            emitterRef.current.removeEventListener('round-ended', handleRoundEnded);
            emitterRef.current.removeEventListener('game-updated', handleGameUpdated);
            emitterRef.current.removeEventListener('game-ended', handleGameEnded);

            emitterRef.current.onopen = null;
            emitterRef.current.onerror = null;

            emitterRef.current.close();
            emitterRef.current = null;
            setIsConnected(false);
        }
    }, [handlePlayerJoined, handlePlayerLeft, handleGameStarted, handleLobbyCreated, handleLobbyUpdated, handleLobbyClosed,
        handleTurnChanged, handleDiceRolled, handleRoundEnded, handleGameUpdated, handleGameEnded]);

    return (
        <SSEContext.Provider value={{
            connectToLobby,
            connectToAllLobbies,
            connectToGame,
            disconnect,
            isConnected,
            registerLobbyHandler,
            registerAllLobbiesHandler,
            registerGameHandler,
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

