import * as React from 'react';
import {createBrowserRouter, Navigate, Outlet} from 'react-router-dom';
import {Home} from './components/generic/home';
import {Login} from './components/auth/login';
import {Signup} from "./components/auth/signup";
import {Lobbies} from './components/lobby/lobbies';
import {LobbyDetails} from './components/lobby/LobbyDetails';
import {Navbar} from './components/generic/navbar';
import {RequireAuthentication} from './components/auth/requireAuthentication';
import {About} from './components/generic/about';
import {Profile} from './components/users/profile';
import {Game} from './components/game/Game';

// Layout component that includes the navbar
import {userService} from './services/userService';
import {isOk} from './services/utils';
import {ToastContainer, useToast} from './components/generic/Toast';

function Layout() {
    const {toasts, removeToast, showSuccess, showError} = useToast();

    const handleEasterEgg = async () => {
        const result = await userService.claimEasterEgg();
        if (isOk(result)) {
            showSuccess(`🐣 Easter Egg Found! +1000 💰 (New Balance: ${result.value})`);
        } else {
            // Silently fail or show error if authenticated check fails
            if (result.error !== 'Session expired') {
                 // Nothing to do, just silently ignore.
            }
        }
    };

    return (
        <>
            <Navbar/>
            <ToastContainer toasts={toasts} removeToast={removeToast} />
            <Outlet/>
            <div 
                onClick={handleEasterEgg}
                style={{
                    position: 'fixed',
                    bottom: '0',
                    right: '0',
                    width: '50px',
                    height: '50px',
                    opacity: 0, // Fully transparent but interacting
                    cursor: 'copy', // Subtle hint cursor
                    zIndex: 2147483647 // Max z-index
                }}
                title="."
            />
        </>
    );
}

/**
 * Route definitions for PokerDice application
 * All possible routes are defined here
 */
export const router = createBrowserRouter([
    {
        element: <Layout/>,
        children: [
            {
                path: '/',
                element: <Navigate to="/home" replace/>,
            },
            {
                path: '/home',
                element: <Home/>,
            },
            {
                path: '/login',
                element: <Login/>,
            },
            {
                path: '/signup',
                element: <Signup/>
            },
            {
                path: '/about',
                element: <About/>
            },
            {
                path: '/profile',
                element: <RequireAuthentication><Profile/></RequireAuthentication>
            },
            {
                path: '/lobbies',
                element: <RequireAuthentication><Lobbies/></RequireAuthentication>
            },
            {
                path: '/lobbies/:lobbyId',
                element: <RequireAuthentication><LobbyDetails/></RequireAuthentication>
            },
            {
                path: '/games/:gameId',
                element: <RequireAuthentication><Game/></RequireAuthentication>
            }
        ]
    }
]);

export default router;
