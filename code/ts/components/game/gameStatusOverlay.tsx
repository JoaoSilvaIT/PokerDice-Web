import React from 'react';
import { ToastContainer } from '../generic/toast';
import '../../styles/game.css';
import { GameDetails } from '../../services/gameService';

interface GameStatusOverlayProps {
    game: GameDetails;
    hostId: number | null;
    currentUserId: number;
    onStartGame: () => void;
    onLeaveGame: () => void;
    toasts: any[];
    removeToast: (id: number) => void;
}

export const GameStatusOverlay: React.FC<GameStatusOverlayProps> = ({
    game,
    hostId,
    currentUserId,
    onStartGame,
    onLeaveGame,
    toasts,
    removeToast
}) => {
    if (game.state === 'WAITING') {
        const isHost = hostId === currentUserId;
        return (
            <div className="game-container">
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className="waiting-overlay">
                    <div className="waiting-content">
                        <h1>WAITING FOR GAME TO START</h1>
                        <div className="waiting-info">
                            <p>Game #{game.id}</p>
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
                            <button onClick={onStartGame} className="start-game-button">
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
        const winners = [...game.players].sort((a, b) => b.moneyWon - a.moneyWon);
        const winner = winners[0];
        const eligiblePlayers = game.players.filter(p => p.currentBalance >= 10);
        const isBankruptcy = eligiblePlayers.length < 2;
        const roundsPlayed = game.currentRound?.number || 0;
        const isPremature = roundsPlayed < game.numberOfRounds;

        return (
            <div className="game-container">
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className="waiting-overlay">
                    <div className="waiting-content">
                        <h1> GAME OVER </h1>
                        
                        {(isBankruptcy || isPremature) && (
                            <div className="game-over-reason">
                                <h3>🚫 Game Ended Early</h3>
                                <p>Not enough players can afford the Ante (10 💰) to continue.</p>
                            </div>
                        )}

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
                        <button onClick={onLeaveGame} className="leave-button">
                            Return to Lobby List
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    if (game.state === 'RUNNING' && !game.currentRound) {
         return (
            <div className="game-container">
                <ToastContainer toasts={toasts} removeToast={removeToast} />
                <div className="waiting-overlay">
                    <div className="waiting-content">
                        <h1>Round starting...</h1>
                        <div className="waiting-info">
                            <p>Game #{game.id}</p>
                            <p>Preparing the first round...</p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return null;
};