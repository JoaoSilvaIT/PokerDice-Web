import React from 'react';
// @ts-ignore
import styles from '../../styles/game.module.css';
import { PlayerInGame } from '../../services/gameService';

interface PlayerSeatProps {
    player: PlayerInGame;
    isCurrentTurn: boolean;
    isCurrentUser: boolean;
}

export const PlayerSeat: React.FC<PlayerSeatProps> = ({ player, isCurrentTurn, isCurrentUser }) => {
    return (
        <div
            className={`${styles['player-seat']} ${isCurrentTurn ? styles['active-turn'] : ''} ${isCurrentUser ? styles['current-user'] : ''}`}
        >
            <div className={styles['player-avatar']}>
                {player.name.charAt(0).toUpperCase()}
            </div>
            <div className={styles['player-name']}>
                {player.name}
                {isCurrentUser && ' (You)'}
            </div>
            {player.handRank && (
                <div className={styles['player-hand-rank']}>
                    {player.handRank}
                </div>
            )}
            <div className={styles['player-chips']}>💰 {player.currentBalance}</div>
            {isCurrentTurn && (
                <div className={styles['turn-indicator']}>
                    {isCurrentUser ? '🎲 Your Turn!' : 'Playing...'}
                </div>
            )}
        </div>
    );
};
