import * as React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthentication } from '../../providers/authentication';
import '../../styles/navbar.css';
import {authService} from "../../services/authService";

export function Navbar() {
    const [username, , clearUsername] = useAuthentication();
    const navigate = useNavigate();

    async function handleLogout() {
        const result = await authService.logout();
        if (result.success) {
            clearUsername();
            navigate('/home');
        } else {
            console.error('Logout failed');
        }
    }

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <h2>PokerDice</h2>
            </div>
            <div className="navbar-links">
                <Link to="/">Home</Link>
                <Link to="/lobbies">Lobbies</Link>
                <Link to="/about">About</Link>
                {username ? (
                    <>
                        <Link to="/profile" className="navbar-username">Welcome, {username}</Link>
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
