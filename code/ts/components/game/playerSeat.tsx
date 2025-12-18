import React from 'react';
import '../../styles/game.css';
import { PlayerInGame } from '../../services/gameService';

interface PlayerSeatProps {
    player: PlayerInGame;
    isCurrentTurn: boolean;
    isCurrentUser: boolean;
    isSpectator?: boolean;
}

export const PlayerSeat: React.FC<PlayerSeatProps> = ({ player, isCurrentTurn, isCurrentUser, isSpectator }) => {
    return (
        <div
            className={`player-seat ${isCurrentTurn ? 'active-turn' : ''} ${isCurrentUser ? 'current-user' : ''} ${isSpectator ? 'spectator' : ''}`}
        >
            <div className="player-avatar">
                {player.name.charAt(0).toUpperCase()}
            </div>
            <div className="player-name">
                {player.name}
                {isCurrentUser && ' (You)'}
            </div>
            {!isSpectator && player.handRank && (
                <div className="player-hand-rank">
                    {player.handRank}
                </div>
            )}
            <div className="player-chips">💰 {player.currentBalance}</div>
            {isCurrentTurn && (
                <div className="turn-indicator">
                    {isCurrentUser ? '🎲 Your Turn!' : 'Playing...'}
                </div>
            )}
        </div>
    );
};