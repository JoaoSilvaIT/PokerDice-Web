import * as React from 'react';
import {NavLink, useNavigate} from 'react-router-dom';
import {useAuthentication} from '../../providers/authentication';
import '../../styles/navbar.css';
import {authService} from "../../services/authService";

export function Navbar() {
    const [username, , clearUsername] = useAuthentication();
    const navigate = useNavigate();

    async function handleLogout() {
        try {
            await authService.logout();
        } catch (e) {
            console.warn('Logout server request failed, clearing local session anyway', e);
        } finally {
            clearUsername();
            navigate('/home');
        }
    }

    const getLinkClass = ({ isActive }: { isActive: boolean }) => 
        isActive ? "nav-link active" : "nav-link";

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <h2>PokerDice</h2>
            </div>
            <div className="navbar-links">
                <NavLink to="/home" className={getLinkClass}>Home</NavLink>
                <NavLink to="/lobbies" className={getLinkClass}>Lobbies</NavLink>
                <NavLink to="/about" className={getLinkClass}>About</NavLink>
                {username ? (
                    <>
                        <NavLink to="/profile" className={getLinkClass}>Welcome, {username}</NavLink>
                        <button onClick={handleLogout} className="navbar-logout">Logout</button>
                    </>
                ) : (
                    <>
                        <NavLink to="/login" className={getLinkClass}>Login</NavLink>
                        <NavLink to="/signup" className={getLinkClass}>Sign Up</NavLink>
                    </>
                )}
            </div>
        </nav>
    );
}
