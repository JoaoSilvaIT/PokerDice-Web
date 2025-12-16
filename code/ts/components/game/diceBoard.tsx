import React from 'react';
// @ts-ignore
import styles from '../../styles/game.module.css';

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
                                onClick={() => onToggleSelect(i)}
                            >
                                {die}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {!isMyTurn && currentTurnPlayerName && (
                <div className={styles['waiting-turn']}>
                    ⏳ Waiting for {currentTurnPlayerName}'s turn...
                </div>
            )}

            {isMyTurn && (
                <div className={styles['dice-info']}>
                    🎲 Rolls Left: {rollsLeft} | Dice Kept : {keptDice.length}/5
                </div>
            )}
        </div>
    );
};
