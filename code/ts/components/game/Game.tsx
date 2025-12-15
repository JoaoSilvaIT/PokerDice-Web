import React, {useEffect, useState} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {gameService, GameDetails} from '../../services/gameService';
import {lobbyService} from '../../services/lobbyService';
import {isOk} from '../../services/utils';
import {useSSE} from '../../providers/SSEContext';
import '../../styles/game.css';

export function Game() {
    const {gameId} = useParams<{ gameId: string }>();
    const navigate = useNavigate();
    const [game, setGame] = useState<GameDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [hostId, setHostId] = useState<number | null>(null);
    const currentUserId = parseInt(localStorage.getItem('userId') || '0');
    const {connectToGame, disconnect, registerGameHandler, unregisterHandler} = useSSE();

    // New state for game controls
    const [rolledDice, setRolledDice] = useState<string[]>([]);
    const [selectedIndices, setSelectedIndices] = useState<number[]>([]);
    const [roundStarting, setRoundStarting] = useState(false);
    const [betAmount, setBetAmount] = useState<number>(10);
    const [lastRoundWinnerId, setLastRoundWinnerId] = useState<number | null>(null);

    useEffect(() => {
        if (lastRoundWinnerId) {
            const timer = setTimeout(() => {
                setLastRoundWinnerId(null);
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [lastRoundWinnerId]);

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
                fetchGame();
                setRolledDice([]);
                setSelectedIndices([]);
            },
            // onDiceRolled
            (event) => {
                fetchGame();
            },
            // onRoundEnded
            (event) => {
                fetchGame();
                setRolledDice([]);
                setSelectedIndices([]);
                setLastRoundWinnerId(event.winnerId);
            },
            // onGameUpdated
            (event) => {
                fetchGame();
            },
            // onGameEnded
            (event) => {
                fetchGame();
            }
        );

        // Connect to game SSE
        connectToGame(gameIdNum).catch((error) => {
            console.error('Failed to connect to game SSE:', error);
        });

        return () => {
            unregisterHandler();
            disconnect('game');
        };
    }, [gameId]);

    const handleStartGame = async () => {
        if (!gameId) return;

        const result = await gameService.startGame(parseInt(gameId));

        if (isOk(result)) {
            setGame(result.value); // Update game state to RUNNING
            // After starting the game, automatically start the first round
            await initializeRound(parseInt(gameId));
        } else {
            setError(result.error || 'Failed to start game');
        }
    };

    const initializeRound = async (gId: number) => {
        if (roundStarting) return;
        setRoundStarting(true);

        try {
            // Start the round without ante - players will set it in betting phase
            await gameService.startRound(gId);
            // Refresh game state
            const gameRes = await gameService.getGame(gId);
            if (isOk(gameRes)) {
                setGame(gameRes.value);
            }
        } finally {
            setRoundStarting(false);
        }
    };

    // Effect to automatically initialize round when game is RUNNING but no round
    useEffect(() => {
        if (game && game.state === 'RUNNING' && !game.currentRound && hostId === currentUserId && !roundStarting) {
            initializeRound(parseInt(gameId!));
        }
    }, [game, hostId, currentUserId, gameId, roundStarting]);

    const handleRollDice = async () => {
        if (!gameId) return;
        setRolledDice([]);
        setSelectedIndices([]);

        const result = await gameService.rollDices(parseInt(gameId));
        if (isOk(result)) {
            setRolledDice(result.value.dice);
            // Refresh game to show updated roll count
            const gameRes = await gameService.getGame(parseInt(gameId));
            if (isOk(gameRes)) setGame(gameRes.value);
        } else {
            setError(result.error || 'Failed to roll dice');
        }
    };

    const handleToggleSelect = (index: number) => {
        if (selectedIndices.includes(index)) {
            setSelectedIndices(prev => prev.filter(i => i !== index));
        } else {
            setSelectedIndices(prev => [...prev, index]);
        }
    };

    const handleHoldSelected = async () => {
        if (!gameId || selectedIndices.length === 0) return;

        const diceToKeep = selectedIndices.map(i => rolledDice[i]);

        // Sequential update (since backend limitation)
        for (const die of diceToKeep) {
            const result = await gameService.updateTurn(parseInt(gameId), die);
            if (!isOk(result)) {
                setError(result.error || 'Failed to hold die');
                return;
            }
        }

        setRolledDice([]);
        setSelectedIndices([]);

        const gameRes = await gameService.getGame(parseInt(gameId));
        if (isOk(gameRes)) setGame(gameRes.value);
    };

    const handleFinishTurn = async () => {
        if (!gameId) return;
        await gameService.nextTurn(parseInt(gameId));
        setRolledDice([]);
        setSelectedIndices([]);
        const gameRes = await gameService.getGame(parseInt(gameId));
        if (isOk(gameRes)) setGame(gameRes.value);
    };

    const handlePlaceBet = async () => {
        if (!gameId) return;

        if (betAmount < 10) {
            setError("Minimum bet is 10");
            return;
        }

        // Set ante for the existing round
        const anteResult = await gameService.setAnte(parseInt(gameId), betAmount);
        if (!isOk(anteResult)) {
            setError(anteResult.error || 'Failed to set ante');
            return;
        }

        // Pay ante
        const payResult = await gameService.payAnte(parseInt(gameId));
        if (!isOk(payResult)) {
            setError(payResult.error || 'Failed to pay ante');
            return;
        }

        const gameRes = await gameService.getGame(parseInt(gameId));
        if (isOk(gameRes)) setGame(gameRes.value);
    };

    const handleLeaveGame = async () => {
        navigate('/lobbies');
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
                                Start Game
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

    if (game.state === 'FINISHED') {
        // Sort players by money won
        const winners = [...game.players].sort((a, b) => b.moneyWon - a.moneyWon);
        const winner = winners[0];

        return (
            <div className="game-container">
                <div className="waiting-overlay">
                    <div className="waiting-content">
                        <h1> GAME OVER </h1>
                        <div className="winner-display">
                            <h2>Winner: {winner.name}</h2>
                            <p className="winner-money">Total Won: 💰 {winner.moneyWon}</p>
                        </div>
                        <div className="waiting-players">
                            <h3>Results</h3>
                            <div className="waiting-players-list">
                                {winners.map((player, index) => (
                                    <div key={player.id} className="waiting-player">
                                        <span className="rank">#{index + 1}</span>
                                        <span className="waiting-player-avatar">
                                            {player.name.charAt(0).toUpperCase()}
                                        </span>
                                        <span className="waiting-player-name">
                                            {player.name}
                                        </span>
                                        <span className="player-money">
                                            💰 {player.moneyWon}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                        <button onClick={handleLeaveGame} className="leave-button">
                            Return to Lobby List
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // Show loading state when game is RUNNING but round not initialized
    if (game.state === 'RUNNING' && !game.currentRound) {
        return (
            <div className="game-container">
                <div className="waiting-overlay">
                    <div className="waiting-content">
                        <h1>Round starting...</h1>
                        <div className="waiting-info">
                            <p>Game #{gameId}</p>
                            <p>Preparing the first round...</p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    const currentRoundNumber = game.currentRound?.number || 0;
    const players = game.players;
    // Use pot from backend if available, otherwise calculate fallback
    const pot = (game.currentRound?.pot !== undefined)
        ? game.currentRound.pot
        : (game.currentRound?.ante ? game.currentRound.ante * players.length : 0);
    const ante = game.currentRound?.ante || 0;
    const currentPlayerId = game.currentRound?.turnUserId;
    const isMyTurn = currentPlayerId === currentUserId;
    const rollsLeft = game.currentRound?.rollsLeft ?? 3;
    const keptDice = game.currentRound?.currentDice || [];

    // Betting phase is when ante has not been set yet (not when pot is 0)
    // This ensures betting interface appears at the start of EACH round
    const isBettingPhase = ante === 0;

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
                {/* Round Winner Banner */}
                {lastRoundWinnerId && (
                    <div className="winner-banner">
                         Round Winner: {players.find(p => p.id === lastRoundWinnerId)?.name}
                    </div>
                )}

                {/* Current Turn Banner */}
                {game.currentRound && (
                    <div className={`turn-banner ${isMyTurn ? 'your-turn' : ''}`}>
                        {isMyTurn ? (
                            <span>🎲 It's Your Turn 🎲</span>
                        ) : (
                            <span> Turn : {players.find(p => p.id === currentPlayerId)?.name}</span>
                        )}
                    </div>
                )}

                {/* Players */}
                {players.map((player) => (
                    <div
                        key={player.id}
                        className={`player-seat ${currentPlayerId === player.id ? 'active-turn' : ''} ${player.id === currentUserId ? 'current-user' : ''}`}
                    >
                        <div className="player-avatar">{player.name.charAt(0).toUpperCase()}</div>
                        <div className="player-name">
                            {player.name}
                            {player.id === currentUserId && ' (You)'}
                        </div>
                        <div className="player-chips">💰 {player.currentBalance}</div>
                        {currentPlayerId === player.id && (
                            <div className="turn-indicator">
                                {player.id === currentUserId ? '🎲 Your Turn!' : 'Playing...'}
                            </div>
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

                    {/* BETTING INTERFACE */}
                    {isBettingPhase ? (
                        <div className="betting-interface">
                            <h3>Place Your Bet</h3>
                            {isMyTurn ? (
                                <div className="bet-controls">
                                    <input
                                        type="number"
                                        min="10"
                                        step="10"
                                        value={betAmount}
                                        onChange={(e) => setBetAmount(parseInt(e.target.value))}
                                        className="bet-input"
                                    />
                                    <button onClick={handlePlaceBet} className="bet-button">
                                        💰 Place Bet
                                    </button>
                                </div>
                            ) : (
                                <div className="waiting-bet">
                                    ⏳ Waiting for {players.find(p => p.id === currentPlayerId)?.name} to place bet...
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="dice-area">
                            {/* KEPT DICE (Persisted) */}
                            <div className="kept-dice-container">
                                <h4>Kept Dice:</h4>
                                <div className="dice-row">
                                    {keptDice.map((die, i) => (
                                        <div key={`kept-${i}`} className="dice kept">{die}</div>
                                    ))}
                                    {keptDice.length === 0 && <span className="no-dice">No dice kept</span>}
                                </div>
                            </div>

                            {/* ROLLED DICE (Transient) */}
                            {rolledDice.length > 0 && (
                                <div className="rolled-dice-container">
                                    <h4>Rolled (Select to Keep):</h4>
                                    <div className="dice-row">
                                        {rolledDice.map((die, i) => (
                                            <div
                                                key={`rolled-${i}`}
                                                className={`dice selectable ${selectedIndices.includes(i) ? 'selected' : ''}`}
                                                onClick={() => handleToggleSelect(i)}
                                            >
                                                {die}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {!isMyTurn && currentPlayerId && (
                                <div className="waiting-turn">
                                    ⏳ Waiting for your turn
                                    de {players.find(p => p.id === currentPlayerId)?.name || 'outro jogador'}...
                                </div>
                            )}

                            {isMyTurn && (
                                <div className="dice-info">
                                    🎲 Rolls Left: {rollsLeft} | Dice Kept : {keptDice.length}/5
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Game Controls */}
            {!isBettingPhase && (
                <div className="game-controls">
                    <button
                        className="game-button roll-button"
                        onClick={handleRollDice}
                        disabled={!isMyTurn || rollsLeft <= 0 || rolledDice.length > 0 || keptDice.length >= 5}
                    >
                        Roll Dice
                    </button>
                    <button
                        className="game-button hold-button"
                        onClick={handleHoldSelected}
                        disabled={!isMyTurn || selectedIndices.length === 0}
                    >
                        Hold Selected
                    </button>
                    <button
                        className="game-button finish-turn-button"
                        onClick={handleFinishTurn}
                        disabled={!isMyTurn || keptDice.length < 5}
                    >
                        Finish Turn
                    </button>
                </div>
            )}
        </div>
    );
}