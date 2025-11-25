import * as React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthentication } from '../../providers/authentication';
import '../../styles/navbar.css';

export function Navbar() {
    const [username, , clearUsername] = useAuthentication();
    const navigate = useNavigate();

    function handleLogout() {
        // Clear the token cookie
        document.cookie = 'token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        // Clear username from authentication context
        clearUsername();
        // Redirect to home
        navigate('/home');
    }

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <h2>PokerDice</h2>
            </div>
            <div className="navbar-links">
                <Link to="/">Home</Link>
                <Link to="/lobbies">Lobbies</Link>
                {username ? (
                    <>
                        <span className="navbar-username">Welcome, {username}</span>
                        <button onClick={handleLogout} className="navbar-logout">Logout</button>
                    </>
                ) : (
                    <>
                        <Link to="/login">Login</Link>
                        <Link to="/signup">Sign Up</Link>
                    </>
                )}
            </div>
        </nav>
    );
}
