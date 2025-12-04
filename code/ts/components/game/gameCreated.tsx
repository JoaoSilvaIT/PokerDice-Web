import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameService, GameDetails } from '../../services/gameService';
import { isOk } from '../../services/utils';
import '../../styles/game.css';

export function Game() {
    const { gameId } = useParams<{ gameId: string }>();
    const navigate = useNavigate();
    const [game, setGame] = useState<GameDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!gameId) {
            setError('Game ID not found');
            setLoading(false);
            return;
        }

        const fetchGame = async () => {
            const result = await gameService.getGame(parseInt(gameId));

            if (isOk(result)) {
                setGame(result.value);
                setError(null);
            } else {
                setError(result.error || 'Failed to fetch game');
            }
            setLoading(false);
        };

        fetchGame();
    }, [gameId]);

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

    const currentRoundNumber = game.currentRound?.number || 0;
    const pot = game.currentRound?.ante || 0;

    return (
        <div className="game-container">
            <div className="game-header">
                <h1 className="game-title">Poker Dice - Game #{gameId}</h1>
                <div className="game-info">
                    <span>State: {game.state}</span>
                    <span>Round: {currentRoundNumber}/{game.numberOfRounds}</span>
                    <span>Pot: {pot} chips</span>
                </div>
            </div>

            {/* Game content will go here */}
            <div className="game-content">
                <p>Game is {game.state}</p>
                {game.currentRound && (
                    <div>
                        <p>Current Turn: Player ID {game.currentRound.turnUserId}</p>
                        <p>Ante: {game.currentRound.ante}</p>
                    </div>
                )}
            </div>

            {/* Game controls */}
            <div className="game-controls">
                <button className="game-button roll-button">Roll Dice</button>
                <button className="game-button hold-button">Hold</button>
                <button className="game-button fold-button">Fold</button>
                <button onClick={() => navigate('/lobbies')} className="game-button leave-button">
                    Leave Game
                </button>
            </div>
        </div>
    );
}


