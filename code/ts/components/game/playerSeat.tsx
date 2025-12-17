import React from 'react';
// @ts-ignore
import styles from '../../styles/game.module.css';
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
            className={`${styles['player-seat']} ${isCurrentTurn ? styles['active-turn'] : ''} ${isCurrentUser ? styles['current-user'] : ''} ${isSpectator ? styles['spectator'] : ''}`}
        >
            <div className={styles['player-avatar']}>
                {player.name.charAt(0).toUpperCase()}
            </div>
            <div className={styles['player-name']}>
                {player.name}
                {isCurrentUser && ' (You)'}
            </div>
            {!isSpectator && player.handRank && (
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
