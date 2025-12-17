import React, {useEffect, useState} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {gameService, GameDetails} from '../../services/gameService';
import {lobbyService} from '../../services/lobbyService';
import {isOk, formatError} from '../../services/utils';
import {useSSE} from '../../providers/SSEContext';
import {ToastContainer, useToast} from '../generic/toast';
// @ts-ignore - Vite handles CSS Modules
import styles from '../../styles/game.module.css';

import { PlayerSeat } from './playerSeat';
import { DiceBoard } from './diceBoard';
import { BettingControls } from './bettingControls';
import { GameControls } from './gameControls';
import { GameStatusOverlay } from './gameStatusOverlay';

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
    const [processingAction, setProcessingAction] = useState(false);


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
            async (event) => {
                // Fetch game first to get updated player data
                const result = await gameService.getGame(gameIdNum);
                if (isOk(result)) {
                    setGame(result.value);
                    // Check winners from the round data
                    const winners = result.value.currentRound?.winners;
                    if (winners && winners.length > 1) {
                        showSuccess(`🤝 Round Draw!`);
                    } else {
                        const winner = result.value.players.find(p => p.id === event.winnerId);
                        if (winner) {
                            showSuccess(`🏆 Round Winner: ${winner.name}`);
                        }
                    }
                }
                setRolledDice([]);
                setSelectedIndices([]);
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
        
        // Keep the dice that were NOT selected (so they don't disappear)
        setRolledDice(prev => prev.filter((_, index) => !selectedIndices.includes(index)));
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

        // Check if ALL players have enough balance for the ante (backend validates this too)
        const minPlayerBalance = game?.players.reduce((min, p) => Math.min(min, p.currentBalance), Infinity) ?? 0;
        if (betAmount > minPlayerBalance) {
            const poorestPlayer = game?.players.reduce((min, p) => p.currentBalance < min.currentBalance ? p : min, game.players[0]);
            showError(`💸 Insufficient funds! ${poorestPlayer?.name} only has 💰${poorestPlayer?.currentBalance}, max bet is 💰${minPlayerBalance}`);
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

    // Verify if we should show overlays (Waiting, Finished, Round Starting)
    if (game.state !== 'RUNNING' || !game.currentRound) {
        return (
            <GameStatusOverlay 
                game={game} 
                hostId={hostId} 
                currentUserId={currentUserId} 
                onStartGame={handleStartGame} 
                onLeaveGame={handleLeaveGame}
                toasts={toasts}
                removeToast={removeToast}
            />
        );
    }

    const currentRoundNumber = game.currentRound?.number || 0;
    const players = game.players;
    const pot = (game.currentRound?.pot !== undefined)
        ? game.currentRound.pot
        : (game.currentRound?.ante ? game.currentRound.ante * players.length : 0);
    const ante = game.currentRound?.ante || 0;
    const currentPlayerId = game.currentRound?.turnUserId;
    const isMyTurn = currentPlayerId === currentUserId;
    const rollsLeft = game.currentRound?.rollsLeft ?? 3;
    const keptDice = game.currentRound?.currentDice || [];
    const isBettingPhase = ante === 0;
    const poorestPlayer = game.players.reduce((min, p) => p.currentBalance < min.currentBalance ? p : min, game.players[0]);
    const minPlayerBalance = game.players.reduce((min, p) => Math.min(min, p.currentBalance), Infinity);

    return (
        <div className={styles['game-container']}>
            <div className={styles['game-content']}>
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                
                {/* Game Header */}
                <div className={styles['game-header']}>
                    <h1 className={styles['game-title']}>Poker Dice</h1>
                    <div className={styles['game-info']}>
                        <span className={styles['info-badge']}>Game #{gameId}</span>
                        <span className={styles['info-badge']}>Round {currentRoundNumber}/{game.numberOfRounds}</span>
                        <span className={styles['info-badge']}>State: {game.state}</span>
                    </div>
                </div>

                {/* Poker Table */}
                <div className={styles['poker-table-compact']}>
                    {/* Players */}
                    {players.map((player) => (
                        <PlayerSeat 
                            key={player.id} 
                            player={player} 
                            isCurrentTurn={currentPlayerId === player.id} 
                            isCurrentUser={player.id === currentUserId} 
                        />
                    ))}

                    {/* Center Area */}
                    <div className={styles['table-center']}>
                        <div className={styles['pot-display']}>
                            <div className={styles['pot-label']}>POT</div>
                            <div className={styles['pot-amount']}>💰 {pot}</div>
                            {ante > 0 && <div className={styles['ante-info']}>Ante: {ante}</div>}
                        </div>

                        {isBettingPhase ? (
                            <BettingControls 
                                isMyTurn={isMyTurn}
                                currentTurnPlayerName={players.find(p => p.id === currentPlayerId)?.name}
                                betAmount={betAmount}
                                minPlayerBalance={minPlayerBalance}
                                poorestPlayer={poorestPlayer}
                                processingAction={processingAction}
                                onSetBetAmount={setBetAmount}
                                onPlaceBet={handlePlaceBet}
                            />
                        ) : (
                            <DiceBoard 
                                keptDice={keptDice}
                                rolledDice={rolledDice}
                                selectedIndices={selectedIndices}
                                isMyTurn={isMyTurn}
                                currentTurnPlayerName={players.find(p => p.id === currentPlayerId)?.name}
                                rollsLeft={rollsLeft}
                                onToggleSelect={handleToggleSelect}
                            />
                        )}
                    </div>
                </div>

                {/* Game Controls */}
                {!isBettingPhase && (
                    <GameControls 
                        isMyTurn={isMyTurn}
                        rollsLeft={rollsLeft}
                        hasRolledDice={rolledDice.length > 0}
                        keptDiceCount={keptDice.length}
                        hasSelectedDice={selectedIndices.length > 0}
                        processingAction={processingAction}
                        onRoll={handleRollDice}
                        onHold={handleHoldSelected}
                        onFinish={handleFinishTurn}
                    />
                )}
            </div>
        </div>
    );
}
