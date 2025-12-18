import React from 'react';
import '../../styles/game.css';
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
            <div className="betting-interface betting-centered">
                <h3>Place Your Bet</h3>
                <div className="waiting-bet">
                    ⏳ Waiting for {currentTurnPlayerName} to place bet...
                </div>
            </div>
        );
    }

    const betExceedsLimit = betAmount > minPlayerBalance;

    return (
        <div className="betting-interface betting-centered">
            <h3>Place Your Bet</h3>
            <div className="bet-controls">
                <div className="bet-amount-wrapper">
                    <span className="currency-symbol">💰</span>
                    <input
                        type="number"
                        min="10"
                        max={minPlayerBalance}
                        step="10"
                        value={betAmount}
                        onChange={(e) => onSetBetAmount(parseInt(e.target.value))}
                        className="bet-input"
                    />
                </div>
                <div className="quick-bets prettier-quick-bets">
                    {[10, 20, 50, 100].map(amount => {
                        const isDisabled = amount > minPlayerBalance || processingAction;
                        return (
                            <button
                                key={amount}
                                onClick={() => onSetBetAmount(amount)}
                                className={`quick-bet-btn prettier-quick-bet-btn ${betAmount === amount ? 'active' : ''}`}
                                disabled={isDisabled}
                            >
                                {amount}
                            </button>
                        );
                    })}
                </div>
                <button
                    onClick={onPlaceBet}
                    className="bet-button"
                    disabled={betExceedsLimit || processingAction}
                >
                    💰 Place Bet
                </button>
                {betExceedsLimit && (
                    <div className="bet-warning" style={{ color: '#ff6b6b', marginTop: '8px', fontSize: '14px' }}>
                        ⚠️ Max bet is 💰{minPlayerBalance} ({poorestPlayer?.name} can't afford more)
                    </div>
                )}
            </div>
        </div>
    );
};