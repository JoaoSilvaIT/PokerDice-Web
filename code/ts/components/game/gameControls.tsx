import React from 'react';
// @ts-ignore
import styles from '../../styles/game.module.css';

interface GameControlsProps {
    isMyTurn: boolean;
    rollsLeft: number;
    hasRolledDice: boolean;
    keptDiceCount: number;
    hasSelectedDice: boolean;
    processingAction: boolean;
    onRoll: () => void;
    onHold: () => void;
    onFinish: () => void;
}

export const GameControls: React.FC<GameControlsProps> = ({
    isMyTurn,
    rollsLeft,
    hasRolledDice,
    keptDiceCount,
    hasSelectedDice,
    processingAction,
    onRoll,
    onHold,
    onFinish
}) => {
    return (
        <div className={styles['game-controls']}>
            <button
                className={`${styles['game-button']} ${styles['roll-button']}`}
                onClick={onRoll}
                disabled={!isMyTurn || rollsLeft <= 0 || keptDiceCount >= 5 || processingAction}
            >
                Roll Dice
            </button>
            <button
                className={`${styles['game-button']} ${styles['hold-button']}`}
                onClick={onHold}
                disabled={!isMyTurn || !hasSelectedDice || processingAction}
            >
                Hold Selected
            </button>
            <button
                className={`${styles['game-button']} ${styles['finish-turn-button']}`}
                onClick={onFinish}
                disabled={!isMyTurn || keptDiceCount < 5 || processingAction}
            >
                Finish Turn
            </button>
        </div>
    );
};
