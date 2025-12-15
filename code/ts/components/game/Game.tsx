import React, {useEffect, useState} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {gameService, GameDetails} from '../../services/gameService';
import {lobbyService} from '../../services/lobbyService';
import {isOk, formatError} from '../../services/utils';
import {useSSE} from '../../providers/SSEContext';
import {ToastContainer, useToast} from '../generic/Toast';
import styles from '../../styles/game.module.css';

const gameErrorMap: Record<string, string> = {
    'insufficient-funds': '💸 Insufficient funds! You don\'t have enough balance.',
};

export function Game() {
    const {gameId} = useParams<{ gameId: string }>();
    const navigate = useNavigate();
    const [game, setGame] = useState<GameDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [hostId, setHostId] = useState<number | null>(null);
    const currentUserId = parseInt(localStorage.getItem('userId') || '0');
    const {connectToGame, disconnect, registerGameHandler, unregisterHandler} = useSSE();
    const {toasts, removeToast, showError, showSuccess} = useToast();

    // New state for game controls
    const [rolledDice, setRolledDice] = useState<string[]>([]);
    const [selectedIndices, setSelectedIndices] = useState<number[]>([]);
    const [roundStarting, setRoundStarting] = useState(false);
    const [betAmount, setBetAmount] = useState<number>(10);
    const [lastRoundWinnerId, setLastRoundWinnerId] = useState<number | null>(null);
    const [processingAction, setProcessingAction] = useState(false);

    useEffect(() => {
        if (lastRoundWinnerId && game) {
            const winner = game.players.find(p => p.id === lastRoundWinnerId);
            if (winner) {
                showSuccess(`🏆 Round Winner: ${winner.name}`);
            }
            setLastRoundWinnerId(null);
        }
    }, [lastRoundWinnerId, game]);

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

                // Fetch lobby to get host information (only if lobby still exists)
                if (result.value.lobbyId) {
                    const lobbyResult = await lobbyService.getLobbyDetails(result.value.lobbyId);
                    if (isOk(lobbyResult)) {
                        setHostId(lobbyResult.value.hostId);
                    }
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
            () => {
                fetchGame();
                setRolledDice([]);
                setSelectedIndices([]);
            },
            // onDiceRolled
            () => {
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
            () => {
                fetchGame();
            },
            // onGameEnded
            () => {
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
            showSuccess('🎮 Game started!');
            // After starting the game, automatically start the first round
            await initializeRound(parseInt(gameId));
        } else {
            showError(formatError(result.error || 'Failed to start game', gameErrorMap));
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
        if (!gameId || processingAction) return;

        // Check if it's the player's turn
        const currentPlayerId = game?.currentRound?.turnUserId;
        const isMyTurn = currentPlayerId === currentUserId;

        if (!isMyTurn) {
            showError("It's not your turn!");
            return;
        }

        setProcessingAction(true);
        setRolledDice([]);
        setSelectedIndices([]);

        const result = await gameService.rollDices(parseInt(gameId));
        if (isOk(result)) {
            setRolledDice(result.value.dice);
            showSuccess('Dice rolled!');
            // Refresh game to show updated roll count
            const gameRes = await gameService.getGame(parseInt(gameId));
            if (isOk(gameRes)) setGame(gameRes.value);
        } else {
            showError(formatError(result.error || 'Failed to roll dice', gameErrorMap));
        }
        setProcessingAction(false);
    };

    const handleToggleSelect = (index: number) => {
        if (selectedIndices.includes(index)) {
            setSelectedIndices(prev => prev.filter(i => i !== index));
        } else {
            setSelectedIndices(prev => [...prev, index]);
        }
    };

    const handleHoldSelected = async () => {
        if (!gameId || selectedIndices.length === 0 || processingAction) return;

        const currentPlayerId = game?.currentRound?.turnUserId;
        const isMyTurn = currentPlayerId === currentUserId;

        if (!isMyTurn) {
            showError("It's not your turn!");
            return;
        }

        setProcessingAction(true);
        const diceToKeep = selectedIndices.map(i => rolledDice[i]);

        // Batch update: send all dice at once
                    const result = await gameService.updateTurn(parseInt(gameId), diceToKeep);
                    if (!isOk(result)) {
                        showError(formatError(result.error || 'Failed to hold dice', gameErrorMap));
                        setProcessingAction(false);
                        return;
                    }
        showSuccess(`✅ Held ${diceToKeep.length} dice!`);
        setRolledDice([]);
        setSelectedIndices([]);

        const gameRes = await gameService.getGame(parseInt(gameId));
        if (isOk(gameRes)) setGame(gameRes.value);
        setProcessingAction(false);
    };

    const handleFinishTurn = async () => {
        if (!gameId || processingAction) return;

        setProcessingAction(true);
        const result = await gameService.nextTurn(parseInt(gameId));
        if (isOk(result)) {
            showSuccess('✅ Turn finished!');
            setRolledDice([]);
            setSelectedIndices([]);
            const gameRes = await gameService.getGame(parseInt(gameId));
            if (isOk(gameRes)) setGame(gameRes.value);
        } else {
            showError(formatError(result.error || 'Failed to finish turn', gameErrorMap));
        }
        setProcessingAction(false);
    };

    const handlePlaceBet = async () => {
        if (!gameId || processingAction) return;

        // Check if player has enough balance (the only real error that can happen in browser)
        const currentPlayer = game?.players.find(p => p.id === currentUserId);
        if (currentPlayer && betAmount > currentPlayer.currentBalance) {
            showError(`💸 Insufficient funds! You have 💰${currentPlayer.currentBalance} but tried to bet 💰${betAmount}`);
            return;
        }

        setProcessingAction(true);
        // Set ante for the existing round
        const anteResult = await gameService.setAnte(parseInt(gameId), betAmount);
        if (!isOk(anteResult)) {
            showError(formatError(anteResult.error, gameErrorMap));
            setProcessingAction(false);
            return;
        }

        // Pay ante
        const payResult = await gameService.payAnte(parseInt(gameId));
        if (!isOk(payResult)) {
            showError(formatError(payResult.error, gameErrorMap));
            setProcessingAction(false);
            return;
        }

        showSuccess(`💰 Bet placed: ${betAmount}`);
        const gameRes = await gameService.getGame(parseInt(gameId));
        if (isOk(gameRes)) setGame(gameRes.value);
        setProcessingAction(false);
    };

    const handleLeaveGame = async () => {
        if (!game || !game.lobbyId) {
            navigate('/lobbies');
            return;
        }

        let currentHostId = hostId;
        // If we don't know the host yet, try to fetch it now as a fallback
        if (!currentHostId) {
            const lobbyRes = await lobbyService.getLobbyDetails(game.lobbyId);
            if (isOk(lobbyRes)) {
                currentHostId = lobbyRes.value.hostId;
            }
        }

        // If we are the host, delete the lobby
        if (currentHostId === currentUserId) {
            const result = await lobbyService.deleteLobby(game.lobbyId);
            if (!isOk(result)) {
                // If lobby is already gone (e.g. 404), just proceed
                if (result.error.includes("not found") || result.error.includes("NotFound")) {
                    navigate('/lobbies');
                    return;
                }

                console.error("Failed to delete lobby:", result.error);
                showError(formatError(result.error, gameErrorMap));
                return;
            }
        }
        navigate('/lobbies');
    };

    if (loading) {
        return (
            <div className={styles['game-container']}>
                <div className={styles['game-loading']}>Loading game...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className={styles['game-container']}>
                <div className={styles['game-error']}>Error: {error}</div>
                <button onClick={() => navigate('/lobbies')} className={styles['back-to-lobbies-button']}>
                    Back to Lobbies
                </button>
            </div>
        );
    }

    if (!game) {
        return (
            <div className={styles['game-container']}>
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className={styles['game-error']}>Game not found</div>
                <button onClick={() => navigate('/lobbies')} className={styles['back-to-lobbies-button']}>
                    Back to Lobbies
                </button>
            </div>
        );
    }

    // Show waiting screen if game state is WAITING
    if (game.state === 'WAITING') {
        const isHost = hostId === currentUserId;

        return (
            <div className={styles['game-container']}>
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className={styles['waiting-overlay']}>
                    <div className={styles['waiting-content']}>
                        <h1>WAITING FOR GAME TO START</h1>
                        <div className={styles['waiting-info']}>
                            <p>Game #{gameId}</p>
                            <p>{game.numberOfRounds} Rounds</p>
                        </div>
                        <div className={styles['waiting-players']}>
                            <h3>Players ({game.players.length})</h3>
                            <div className={styles['waiting-players-list']}>
                                {game.players.map((player) => (
                                    <div key={player.id} className={styles['waiting-player']}>
                                        <span className={styles['waiting-player-avatar']}>
                                            {player.name.charAt(0).toUpperCase()}
                                        </span>
                                        <span className={styles['waiting-player-name']}>
                                            {player.name}
                                            {player.id === hostId && ' (Host)'}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                        {isHost && (
                            <button onClick={handleStartGame} className={styles['start-game-button']}>
                                Start Game
                            </button>
                        )}
                        {!isHost && (
                            <p className={styles['waiting-message']}>Waiting for host to start the game...</p>
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
        
        // Check for premature termination (Bankruptcy)
        const eligiblePlayers = game.players.filter(p => p.currentBalance >= 10); // Assuming 10 is MIN_ANTE
        const isBankruptcy = eligiblePlayers.length < 2;
        const roundsPlayed = game.currentRound?.number || 0;
        const isPremature = roundsPlayed < game.numberOfRounds;

        return (
            <div className={styles['game-container']}>
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className={styles['waiting-overlay']}>
                    <div className={styles['waiting-content']}>
                        <h1> GAME OVER </h1>
                        
                        {(isBankruptcy || isPremature) && (
                            <div className={styles['game-over-reason']}>
                                <h3>🚫 Game Ended Early</h3>
                                <p>
                                    Not enough players can afford the Ante (10 💰) to continue.
                                </p>
                            </div>
                        )}

                        <div className={styles['winner-display']}>
                            <h2>Winner: {winner.name}</h2>
                            <p className={styles['winner-money']}>Total Won: 💰 {winner.moneyWon}</p>
                        </div>
                        <div className={styles['waiting-players']}>
                            <h3>Results</h3>
                            <div className={styles['waiting-players-list']}>
                                {winners.map((player, index) => (
                                    <div key={player.id} className={styles['waiting-player']}>
                                        <span className={styles['rank']}>#{index + 1}</span>
                                        <span className={styles['waiting-player-avatar']}>
                                            {player.name.charAt(0).toUpperCase()}
                                        </span>
                                        <span className={styles['waiting-player-name']}>
                                            {player.name}
                                        </span>
                                        <span className={styles['player-money']}>
                                            💰 {player.moneyWon}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                        <button onClick={handleLeaveGame} className={styles['leave-button']}>
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
            <div className={styles['game-container']}>
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className={styles['waiting-overlay']}>
                    <div className={styles['waiting-content']}>
                        <h1>Round starting...</h1>
                        <div className={styles['waiting-info']}>
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
        <div className={styles['game-container']}>
            <ToastContainer toasts={toasts} removeToast={removeToast} />
            {/* Game Header with Info */}
            <div className={styles['game-header']}>
                <h1 className={styles['game-title']}>Poker Dice</h1>
                <div className={styles['game-info']}>
                    <span className={styles['info-badge']}>Game #{gameId}</span>
                    <span className={styles['info-badge']}>Round {currentRoundNumber}/{game.numberOfRounds}</span>
                    <span className={styles['info-badge']}>State: {game.state}</span>
                </div>
            </div>

            {/* Poker Table */}
            <div className={styles['poker-table']}>
                {/* Current Turn Banner */}
                {game.currentRound && (
                    <div className={`${styles['turn-banner']} ${isMyTurn ? styles['your-turn'] : ''}`}>
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
                        className={`${styles['player-seat']} ${currentPlayerId === player.id ? styles['active-turn'] : ''} ${player.id === currentUserId ? styles['current-user'] : ''}`}
                    >
                        <div className={styles['player-avatar']}>{player.name.charAt(0).toUpperCase()}</div>
                        <div className={styles['player-name']}>
                            {player.name}
                            {player.id === currentUserId && ' (You)'}
                        </div>
                        {player.handRank && (
                            <div className={styles['player-hand-rank']}>
                                {player.handRank}
                            </div>
                        )}
                        <div className={styles['player-chips']}>💰 {player.currentBalance}</div>
                        {currentPlayerId === player.id && (
                            <div className={styles['turn-indicator']}>
                                {player.id === currentUserId ? '🎲 Your Turn!' : 'Playing...'}
                            </div>
                        )}
                    </div>
                ))}

                {/* Center Area - Pot and Dice */}
                <div className={styles['table-center']}>
                    <div className={styles['pot-display']}>
                        <div className={styles['pot-label']}>POT</div>
                        <div className={styles['pot-amount']}>💰 {pot}</div>
                        {ante > 0 && <div className={styles['ante-info']}>Ante: {ante}</div>}
                    </div>

                    {/* BETTING INTERFACE */}
                    {isBettingPhase ? (
                        <div className={`${styles['betting-interface']} ${styles['betting-centered']}`}>
                            <h3>Place Your Bet</h3>
                            {isMyTurn ? (
                                <div className={styles['bet-controls']}>
                                    <div className={styles['bet-amount-wrapper']}>
                                        <span className={styles['currency-symbol']}>💰</span>
                                        <input
                                            type="number"
                                            min="10"
                                            step="10"
                                            value={betAmount}
                                            onChange={(e) => setBetAmount(parseInt(e.target.value))}
                                            className={styles['bet-input']}
                                        />
                                    </div>
                                    <div className={`${styles['quick-bets']} ${styles['prettier-quick-bets']}`}>
                                        {[10, 20, 50, 100].map(amount => {
                                            const currentPlayer = game.players.find(p => p.id === currentUserId);
                                            const currentPlayerBalance = currentPlayer ? currentPlayer.currentBalance : 0;
                                            const isDisabled = amount > currentPlayerBalance || processingAction;
                                            return (
                                                <button
                                                    key={amount}
                                                    onClick={() => setBetAmount(amount)}
                                                    className={`${styles['quick-bet-btn']} ${styles['prettier-quick-bet-btn']} ${betAmount === amount ? styles['active'] : ''}`}
                                                    disabled={isDisabled}
                                                >
                                                    {amount}
                                                </button>
                                            );
                                        })}
                                    </div>
                                    <button
                                        onClick={handlePlaceBet}
                                        className={styles['bet-button']}
                                        disabled={betAmount > (game.players.find(p => p.id === currentUserId)?.currentBalance || 0) || processingAction}
                                    >
                                        💰 Place Bet
                                    </button>
                                    {betAmount > (game.players.find(p => p.id === currentUserId)?.currentBalance || 0) && (
                                        <div className={styles['bet-warning']} style={{color: '#ff6b6b', marginTop: '8px', fontSize: '14px'}}>
                                            ⚠️ Insufficient funds! Your balance: 💰{game.players.find(p => p.id === currentUserId)?.currentBalance}
                                        </div>
                                    )}
                                </div>
                            ) : (
                                <div className={styles['waiting-bet']}>
                                    ⏳ Waiting for {players.find(p => p.id === currentPlayerId)?.name} to place bet...
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className={styles['dice-area']}>
                            {/* KEPT DICE (Persisted) */}
                            <div className={styles['kept-dice-container']}>
                                <h4>Kept Dice:</h4>
                                <div className={styles['dice-row']}>
                                    {keptDice.map((die, i) => (
                                        <div key={`kept-${i}`} className={`${styles['dice']} ${styles['kept']}`}>{die}</div>
                                    ))}
                                    {keptDice.length === 0 && <span className={styles['no-dice']}>No dice kept</span>}
                                </div>
                            </div>

                            {/* ROLLED DICE (Transient) */}
                            {rolledDice.length > 0 && (
                                <div className={styles['rolled-dice-container']}>
                                    <h4>Rolled (Select to Keep):</h4>
                                    <div className={styles['dice-row']}>
                                        {rolledDice.map((die, i) => (
                                            <div
                                                key={`rolled-${i}`}
                                                className={`${styles['dice']} ${styles['selectable']} ${selectedIndices.includes(i) ? styles['selected'] : ''}`}
                                                onClick={() => handleToggleSelect(i)}
                                            >
                                                {die}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {!isMyTurn && currentPlayerId && (
                                <div className={styles['waiting-turn']}>
                                    ⏳ Waiting for your turn
                                    de {players.find(p => p.id === currentPlayerId)?.name || 'outro jogador'}...
                                </div>
                            )}

                            {isMyTurn && (
                                <div className={styles['dice-info']}>
                                    🎲 Rolls Left: {rollsLeft} | Dice Kept : {keptDice.length}/5
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Game Controls */}
            {!isBettingPhase && (
                <div className={styles['game-controls']}>
                    <button
                        className={`${styles['game-button']} ${styles['roll-button']}`}
                        onClick={handleRollDice}
                        disabled={!isMyTurn || rollsLeft <= 0 || rolledDice.length > 0 || keptDice.length >= 5 || processingAction}
                    >
                        Roll Dice
                    </button>
                    <button
                        className={`${styles['game-button']} ${styles['hold-button']}`}
                        onClick={handleHoldSelected}
                        disabled={!isMyTurn || selectedIndices.length === 0 || processingAction}
                    >
                        Hold Selected
                    </button>
                    <button
                        className={`${styles['game-button']} ${styles['finish-turn-button']}`}
                        onClick={handleFinishTurn}
                        disabled={!isMyTurn || keptDice.length < 5 || processingAction}
                    >
                        Finish Turn
                    </button>
                </div>
            )}
        </div>
    );
}