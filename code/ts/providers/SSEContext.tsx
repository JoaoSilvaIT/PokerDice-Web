import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { RequestUri } from '../services/requestUri';

// --- Interfaces (Mantidas patestei ra tipagem forte) ---
interface PlayerJoinedEvent { lobbyId: number; userId: number; playerName: string; }
interface PlayerLeftEvent { lobbyId: number; playerId: number; timestamp: string; }
interface LobbyCreatedEvent { lobbyId: number; lobbyName: string; }
interface LobbyUpdatedEvent { lobbyId: number; }
interface LobbyClosedEvent { lobbyId: number; }
interface CountdownStartedEvent { lobbyId: number; expiresAt: number; }
interface CountdownCancelledEvent { lobbyId: number; }
interface GameStartedEvent { lobbyId: number; gameId: number; }
interface TurnChangedEvent { gameId: number; turnUserId: number; roundNumber: number; }
interface DiceRolledEvent { gameId: number; userId: number; dice: string[]; }
interface RoundEndedEvent { gameId: number; roundNumber: number; winnerId: number; }
interface GameUpdatedEvent { gameId: number; }
interface GameEndedEvent { gameId: number; }

// --- Handler Types ---
// Agrupamos os handlers num objeto genérico para flexibilidade
type HandlerMap = Record<string, (data: any) => void>;

interface SSEContextType {
    connectToLobby: (lobbyId: number) => Promise<void>;
    connectToAllLobbies: () => Promise<void>;
    connectToGame: (gameId: number) => Promise<void>;
    disconnect: (type?: 'lobby' | 'all-lobbies' | 'game') => void;
    isConnected: boolean;
    // Registos mantêm a assinatura original para compatibilidade
    registerLobbyHandler: (
        lobbyId: number,
        onPlayerJoined?: (event: PlayerJoinedEvent) => void,
        onPlayerLeft?: (event: PlayerLeftEvent) => void,
        onGameStarted?: (event: GameStartedEvent) => void,
        onCountdownStarted?: (event: CountdownStartedEvent) => void,
        onCountdownCancelled?: (event: CountdownCancelledEvent) => void
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

export function SSEProvider({ children }: { children: React.ReactNode }) {
    const emitterRef = useRef<EventSource | null>(null);
    const connectionType = useRef<'lobby' | 'all-lobbies' | 'game' | null>(null);
    const currentTargetId = useRef<number | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    
    // Armazena os handlers registados atualmente
    const activeHandlers = useRef<HandlerMap>({});

    // --- Core Connection Logic ---
    // Esta função centraliza toda a lógica de conectar, limpar listeners antigos, e adicionar novos.
    const connect = useCallback((
        url: string, 
        type: 'lobby' | 'all-lobbies' | 'game', 
        targetId: number | null,
        eventListeners: Record<string, (e: MessageEvent) => void> // Mapa de nome_evento -> função_wrapper
    ) => {
        return new Promise<void>((resolve, reject) => {
            // 1. Evitar reconexão desnecessária
            if (emitterRef.current && connectionType.current === type && currentTargetId.current === targetId) {
                resolve();
                return;
            }

            // 2. Limpeza total da conexão anterior
            if (emitterRef.current) {
                emitterRef.current.close();
                emitterRef.current = null;
                setIsConnected(false);
            }

            // 3. Nova Conexão
            const source = new EventSource(url, { withCredentials: true });
            emitterRef.current = source;
            connectionType.current = type;
            currentTargetId.current = targetId;

            source.onopen = () => {
                setIsConnected(true);
                resolve();
            };

            source.onerror = (error) => {
                console.error(`[SSE] Error in ${type}:`, error);
                setIsConnected(false);
                source.close();
                // Limpar refs se a conexão morrer
                if (emitterRef.current === source) {
                    emitterRef.current = null;
                    connectionType.current = null;
                }
                reject(error);
            };

            // 4. Ligar Listeners Dinamicamente
            Object.entries(eventListeners).forEach(([event, listener]) => {
                source.addEventListener(event, listener);
            });
        });
    }, []);

    const disconnect = useCallback((type?: 'lobby' | 'all-lobbies' | 'game') => {
        if (type && connectionType.current !== type) return;

        if (emitterRef.current) {
            emitterRef.current.close();
            emitterRef.current = null;
        }
        connectionType.current = null;
        currentTargetId.current = null;
        setIsConnected(false);
        activeHandlers.current = {}; // Limpar handlers
    }, []);

    // --- Helper para criar listeners seguros ---
    // Cria uma função que faz parse do JSON e chama o handler correto se ele existir
    const createListener = <T,>(handlerName: string, idCheck?: (data: T) => boolean) => {
        return (event: MessageEvent) => {
            try {
                const data = JSON.parse(event.data);
                if (idCheck && !idCheck(data)) return; // Ignora eventos de outros IDs se necessário

                const handler = activeHandlers.current[handlerName];
                if (handler) handler(data);
            } catch (e) {
                console.error(`[SSE] Error parsing ${handlerName}:`, e);
            }
        };
    };

    // --- Public Registration Methods ---
    const registerAllLobbiesHandler = useCallback((
        onLobbyCreated?: (e: LobbyCreatedEvent) => void,
        onLobbyUpdated?: (e: LobbyUpdatedEvent) => void,
        onLobbyClosed?: (e: LobbyClosedEvent) => void
    ) => {
        // Atualiza o mapa de handlers ativos
        activeHandlers.current = {
            'new-lobby': onLobbyCreated as any,
            'lobby-updated': onLobbyUpdated as any,
            'lobby-closed': onLobbyClosed as any,
        };
    }, []);

    const connectToAllLobbies = useCallback(() => {
        const listeners = {
            'new-lobby': createListener('new-lobby'),
            'lobby-updated': createListener('lobby-updated'),
            'lobby-closed': createListener('lobby-closed'),
        };
        return connect(RequestUri.lobby.listenAll, 'all-lobbies', null, listeners);
    }, [connect]);

    const registerLobbyHandler = useCallback((
        lobbyId: number,
        onPlayerJoined?: (e: PlayerJoinedEvent) => void,
        onPlayerLeft?: (e: PlayerLeftEvent) => void,
        onGameStarted?: (e: GameStartedEvent) => void,
        onCountdownStarted?: (e: CountdownStartedEvent) => void,
        onCountdownCancelled?: (e: CountdownCancelledEvent) => void
    ) => {
        activeHandlers.current = {
            'player-joined': onPlayerJoined as any,
            'player-left': onPlayerLeft as any,
            'game-started': onGameStarted as any,
            'countdown-started': onCountdownStarted as any,
            'countdown-cancelled': onCountdownCancelled as any,
        };
    }, []);

    const connectToLobby = useCallback((lobbyId: number) => {
        const idCheck = (d: { lobbyId: number }) => d.lobbyId === lobbyId;
        const listeners = {
            'player-joined': createListener('player-joined', idCheck),
            'player-left': createListener('player-left', idCheck),
            'game-started': createListener('game-started', idCheck),
            'countdown-started': createListener('countdown-started', idCheck),
            'countdown-cancelled': createListener('countdown-cancelled', idCheck),
        };
        return connect(RequestUri.lobby.listen(lobbyId), 'lobby', lobbyId, listeners);
    }, [connect]);

    const registerGameHandler = useCallback((
        gameId: number,
        onTurnChanged?: (e: TurnChangedEvent) => void,
        onDiceRolled?: (e: DiceRolledEvent) => void,
        onRoundEnded?: (e: RoundEndedEvent) => void,
        onGameUpdated?: (e: GameUpdatedEvent) => void,
        onGameEnded?: (e: GameEndedEvent) => void
    ) => {
        activeHandlers.current = {
            'turn-changed': onTurnChanged as any,
            'dice-rolled': onDiceRolled as any,
            'round-ended': onRoundEnded as any,
            'game-updated': onGameUpdated as any,
            'game-ended': onGameEnded as any,
        };
    }, []);

    const connectToGame = useCallback((gameId: number) => {
        const idCheck = (d: { gameId: number }) => d.gameId === gameId;
        const listeners = {
            'turn-changed': createListener('turn-changed', idCheck),
            'dice-rolled': createListener('dice-rolled', idCheck),
            'round-ended': createListener('round-ended', idCheck),
            'game-updated': createListener('game-updated', idCheck),
            'game-ended': createListener('game-ended', idCheck),
        };
        return connect(RequestUri.game.listen(gameId), 'game', gameId, listeners);
    }, [connect]);

    const unregisterHandler = useCallback(() => {
        activeHandlers.current = {};
    }, []);

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