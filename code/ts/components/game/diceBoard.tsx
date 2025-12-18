import React from 'react';
import '../../styles/game.css';

interface DiceBoardProps {
    keptDice: string[];
    rolledDice: string[];
    selectedIndices: number[];
    isMyTurn: boolean;
    currentTurnPlayerName?: string;
    rollsLeft: number;
    onToggleSelect: (index: number) => void;
}

export const DiceBoard: React.FC<DiceBoardProps> = ({
    keptDice,
    rolledDice,
    selectedIndices,
    isMyTurn,
    currentTurnPlayerName,
    rollsLeft,
    onToggleSelect
}) => {
    return (
        <div className="dice-area">
            {}
            <div className="kept-dice-container">
                <h4>Kept Dice:</h4>
                <div className="dice-row">
                    {keptDice.map((die, i) => (
                        <div key={`kept-${i}`} className={`dice kept`}>{die}</div>
                    ))}
                    {keptDice.length === 0 && <span className="no-dice">No dice kept</span>}
                </div>
            </div>

            {}
            {rolledDice.length > 0 && (
                <div className="rolled-dice-container">
                    <h4>Rolled (Select to Keep):</h4>
                    <div className="dice-row">
                        {rolledDice.map((die, i) => (
                            <div
                                key={`rolled-${i}`}
                                className={`dice selectable ${selectedIndices.includes(i) ? 'selected' : ''}`}
                                onClick={() => onToggleSelect(i)}
                            >
                                {die}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {!isMyTurn && currentTurnPlayerName && (
                <div className="waiting-turn">
                    ⏳ Waiting for {currentTurnPlayerName}'s turn...
                </div>
            )}

            {isMyTurn && (
                <div className="dice-info">
                    🎲 Rolls Left: {rollsLeft} | Dice Kept : {keptDice.length}/5
                </div>
            )}
        </div>
    );
};