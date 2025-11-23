import * as React from 'react';
import '../../styles/navbar.css';

export function Navbar() {
    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <h2>PokerDice</h2>
            </div>
            <div className="navbar-links">
                <a href="/">Home</a>
                <a href="/lobbies">Lobbies</a>
                <a href="/login">Login</a>
                <a href="/signup">Sign Up</a>
            </div>
        </nav>
    );
}

