import React from 'react';
import '../../styles/game.css';

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
        <div className="game-controls">
            <button
                className="game-button roll-button"
                onClick={onRoll}
                disabled={!isMyTurn || rollsLeft <= 0 || keptDiceCount >= 5 || processingAction}
            >
                Roll Dice
            </button>
            <button
                className="game-button hold-button"
                onClick={onHold}
                disabled={!isMyTurn || !hasSelectedDice || processingAction}
            >
                Hold Selected
            </button>
            <button
                className="game-button finish-turn-button"
                onClick={onFinish}
                disabled={!isMyTurn || keptDiceCount < 5 || processingAction}
            >
                Finish Turn
            </button>
        </div>
    );
};