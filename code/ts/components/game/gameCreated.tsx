import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameService, GameDetails } from '../../services/gameService';
import { lobbyService } from '../../services/lobbyService';
import { isOk } from '../../services/utils';
import { useSSE } from '../../providers/SSEContext';
import '../../styles/game.css';

export function Game() {
    const { gameId } = useParams<{ gameId: string }>();
    const navigate = useNavigate();
    const [game, setGame] = useState<GameDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [hostId, setHostId] = useState<number | null>(null);
    const currentUserId = parseInt(localStorage.getItem('userId') || '0');
    const { connectToGame, disconnect, isConnected, registerGameHandler, unregisterHandler } = useSSE();

    useEffect(() => {
        if (!gameId) {
            setError('Game ID not found');
            setLoading(false);
            return;
        }

        const gameIdNum = parseInt(gameId);

        const fetchGame = async () => {
            const result = await gameService.getGame(gameIdNum);

            if (isOk(result)) {
                setGame(result.value);

                // Fetch lobby to get host information
                const lobbyResult = await lobbyService.getLobbyDetails(result.value.lobbyId);
                if (isOk(lobbyResult)) {
                    setHostId(lobbyResult.value.hostId);
                }

                setError(null);
            } else {
                setError(result.error || 'Failed to fetch game');
            }
            setLoading(false);
        };

        fetchGame();

        // Register SSE event handlers
        registerGameHandler(
            gameIdNum,
            // onTurnChanged
            (event) => {
                console.log('Turn changed:', event);
                fetchGame(); // Refresh game state
            },
            // onDiceRolled
            (event) => {
                console.log('Dice rolled:', event);
                fetchGame(); // Refresh game state
            },
            // onRoundEnded
            (event) => {
                console.log('Round ended:', event);
                fetchGame(); // Refresh game state
            },
            // onGameUpdated
            (event) => {
                console.log('Game updated:', event);
                fetchGame(); // Refresh game state
            },
            // onGameEnded
            (event) => {
                console.log('Game ended:', event);
                fetchGame(); // Refresh game state
            }
        );

        // Connect to game SSE
        connectToGame(gameIdNum).catch((error) => {
            console.error('Failed to connect to game SSE:', error);
        });

        return () => {
            unregisterHandler();
            disconnect();
        };
    }, [gameId]);

    const handleStartGame = async () => {
        if (!gameId) return;

        const result = await gameService.startGame(parseInt(gameId));

        if (isOk(result)) {
            setGame(result.value); // Update game state to RUNNING
        } else {
            setError(result.error || 'Failed to start game');
        }
    };

    if (loading) {
        return (
            <div className="game-container">
                <div className="game-loading">Loading game...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="game-container">
                <div className="game-error">Error: {error}</div>
                <button onClick={() => navigate('/lobbies')} className="back-to-lobbies-button">
                    Back to Lobbies
                </button>
            </div>
        );
    }

    if (!game) {
        return (
            <div className="game-container">
                <div className="game-error">Game not found</div>
                <button onClick={() => navigate('/lobbies')} className="back-to-lobbies-button">
                    Back to Lobbies
                </button>
            </div>
        );
    }

    // Show waiting screen if game state is WAITING
    if (game.state === 'WAITING') {
        const isHost = hostId === currentUserId;

        return (
            <div className="game-container">
                <div className="waiting-overlay">
                    <div className="waiting-content">
                        <h1>WAITING FOR GAME TO START</h1>
                        <div className="waiting-info">
                            <p>Game #{gameId}</p>
                            <p>{game.numberOfRounds} Rounds</p>
                        </div>
                        <div className="waiting-players">
                            <h3>Players ({game.players.length})</h3>
                            <div className="waiting-players-list">
                                {game.players.map((player) => (
                                    <div key={player.id} className="waiting-player">
                                        <span className="waiting-player-avatar">
                                            {player.name.charAt(0).toUpperCase()}
                                        </span>
                                        <span className="waiting-player-name">
                                            {player.name}
                                            {player.id === hostId && ' (Host)'}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                        {isHost && (
                            <button onClick={handleStartGame} className="start-game-button">
                                🎮 Start Game
                            </button>
                        )}
                        {!isHost && (
                            <p className="waiting-message">Waiting for host to start the game...</p>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    const currentRoundNumber = game.currentRound?.number || 0;
    const players = game.players;
    const pot = game.currentRound?.ante ? game.currentRound.ante * players.length : 0;
    const ante = game.currentRound?.ante || 0;
    const currentPlayerId = game.currentRound?.turnUserId;

    return (
        <div className="game-container">
            {/* Game Header with Info */}
            <div className="game-header">
                <h1 className="game-title">Poker Dice</h1>
                <div className="game-info">
                    <span className="info-badge">Game #{gameId}</span>
                    <span className="info-badge">Round {currentRoundNumber}/{game.numberOfRounds}</span>
                    <span className="info-badge">State: {game.state}</span>
                </div>
            </div>

            {/* Poker Table */}
            <div className="poker-table">
                {/* Players */}
                {players.map((player) => (
                    <div
                        key={player.id}
                        className={`player-seat ${currentPlayerId === player.id ? 'active-turn' : ''}`}
                    >
                        <div className="player-avatar">{player.name.charAt(0).toUpperCase()}</div>
                        <div className="player-name">{player.name}</div>
                        <div className="player-chips">💰 {player.currentBalance}</div>
                        {currentPlayerId === player.id && (
                            <div className="turn-indicator">Your Turn</div>
                        )}
                    </div>
                ))}

                {/* Center Area - Pot and Dice */}
                <div className="table-center">
                    <div className="pot-display">
                        <div className="pot-label">POT</div>
                        <div className="pot-amount">💰 {pot}</div>
                        {ante > 0 && <div className="ante-info">Ante: {ante}</div>}
                    </div>

                    <div className="dice-area">
                        {/* Placeholder for dice - will be dynamic later */}
                        <div className="dice-placeholder">
                            <div className="dice">🎲</div>
                            <div className="dice">🎲</div>
                            <div className="dice">🎲</div>
                            <div className="dice">🎲</div>
                            <div className="dice">🎲</div>
                        </div>
                        <div className="dice-info">Rolls left: 3</div>
                    </div>
                </div>
            </div>

            {/* Game Controls */}
            <div className="game-controls">
                <button className="game-button roll-button">
                    🎲 Roll Dice
                </button>
                <button className="game-button hold-button">
                    ✋ Hold Selected
                </button>
                <button className="game-button fold-button">
                    ❌ Fold
                </button>
                <button onClick={() => navigate('/lobbies')} className="game-button leave-button">
                    🚪 Leave Game
                </button>
            </div>
        </div>
    );
}


