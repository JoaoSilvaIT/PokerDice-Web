import * as React from 'react';
import '../../styles/about.css';

export function About() {
    return (
        <div className="about-container">
            <div className="about-content">
                <h1>About PokerDice</h1>

                <section>
                    <h2>The Game</h2>
                    <p>
                        PokerDice is a multiplayer dice game that combines the mechanics of poker with the excitement of
                        dice rolling.
                        Players compete in matches organized in lobbies, testing their luck and strategy.
                    </p>
                </section>

                <section>
                    <h2>Rules</h2>
                    <p>
                        The game is played with 5 dice, each having six faces: Ace, King, Queen, Jack, 10, and 9.
                    </p>
                    <p>
                        <strong>Hand Rankings (Highest to Lowest):</strong>
                    </p>
                    <ul>
                        <li>Five of a Kind</li>
                        <li>Four of a Kind</li>
                        <li>Full House</li>
                        <li>Straight</li>
                        <li>Three of a Kind</li>
                        <li>Two Pairs</li>
                        <li>One Pair</li>
                        <li>Bust (High Card)</li>
                    </ul>
                </section>

                <section>
                    <h2>How to Play</h2>
                    <p>
                        <strong>1. Join a Lobby:</strong> Create or join a lobby to start a match.
                    </p>
                    <p>
                        <strong>2. Ante Up:</strong> Each round begins with players paying an 'ante' from their balance.
                    </p>
                    <p>
                        <strong>3. Roll & Hold:</strong> You have up to 3 rolls per turn. After the first roll, you can
                        choose to hold specific dice and re-roll the others to improve your hand.
                    </p>
                    <p>
                        <strong>4. Win:</strong> The player with the highest-ranking hand at the end of the round wins
                        the pot!
                    </p>
                </section>

                <section>
                    <h2>Authors</h2>
                    <p>
                        Developed by Group 6 - DAW 2025/2026
                        <br/>
                        <br/>
                        Bernardo Jaco 51690
                        <br/>
                        João Silva 51682
                        <br/>
                        Pedro Monteiro 51457
                    </p>
                </section>
            </div>
        </div>
    );
}
