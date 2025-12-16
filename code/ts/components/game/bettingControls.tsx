import React from 'react';
// @ts-ignore
import styles from '../../styles/game.module.css';
import { PlayerInGame } from '../../services/gameService';

interface BettingControlsProps {
    isMyTurn: boolean;
    currentTurnPlayerName?: string;
    betAmount: number;
    minPlayerBalance: number;
    poorestPlayer?: PlayerInGame;
    processingAction: boolean;
    onSetBetAmount: (amount: number) => void;
    onPlaceBet: () => void;
}

export const BettingControls: React.FC<BettingControlsProps> = ({
    isMyTurn,
    currentTurnPlayerName,
    betAmount,
    minPlayerBalance,
    poorestPlayer,
    processingAction,
    onSetBetAmount,
    onPlaceBet
}) => {
    if (!isMyTurn) {
        return (
            <div className={`${styles['betting-interface']} ${styles['betting-centered']}`}>
                <h3>Place Your Bet</h3>
                <div className={styles['waiting-bet']}>
                    ⏳ Waiting for {currentTurnPlayerName} to place bet...
                </div>
            </div>
        );
    }

    const betExceedsLimit = betAmount > minPlayerBalance;

    return (
        <div className={`${styles['betting-interface']} ${styles['betting-centered']}`}>
            <h3>Place Your Bet</h3>
            <div className={styles['bet-controls']}>
                <div className={styles['bet-amount-wrapper']}>
                    <span className={styles['currency-symbol']}>💰</span>
                    <input
                        type="number"
                        min="10"
                        max={minPlayerBalance}
                        step="10"
                        value={betAmount}
                        onChange={(e) => onSetBetAmount(parseInt(e.target.value))}
                        className={styles['bet-input']}
                    />
                </div>
                <div className={`${styles['quick-bets']} ${styles['prettier-quick-bets']}`}>
                    {[10, 20, 50, 100].map(amount => {
                        const isDisabled = amount > minPlayerBalance || processingAction;
                        return (
                            <button
                                key={amount}
                                onClick={() => onSetBetAmount(amount)}
                                className={`${styles['quick-bet-btn']} ${styles['prettier-quick-bet-btn']} ${betAmount === amount ? styles['active'] : ''}`}
                                disabled={isDisabled}
                            >
                                {amount}
                            </button>
                        );
                    })}
                </div>
                <button
                    onClick={onPlaceBet}
                    className={styles['bet-button']}
                    disabled={betExceedsLimit || processingAction}
                >
                    💰 Place Bet
                </button>
                {betExceedsLimit && (
                    <div className={styles['bet-warning']} style={{ color: '#ff6b6b', marginTop: '8px', fontSize: '14px' }}>
                        ⚠️ Max bet is 💰{minPlayerBalance} ({poorestPlayer?.name} can't afford more)
                    </div>
                )}
            </div>
        </div>
    );
};
