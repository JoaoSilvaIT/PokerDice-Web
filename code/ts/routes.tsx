import * as React from 'react';
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';
import { Home } from './components/generic/home';
import { Login } from './components/auth/login';
import {Signup} from "./components/auth/signup";
import { Lobbies } from './components/lobby/lobbies';
import { LobbyDetails } from './components/lobby/LobbyDetails';
import { Navbar } from './components/generic/navbar';

// Layout component that includes the navbar
function Layout() {
    return (
        <>
            <Navbar />
            <Outlet />
        </>
    );
}

/**
 * Route definitions for PokerDice application
 * All possible routes are defined here
 */
export const router = createBrowserRouter([
    {
        element: <Layout />,
        children: [
            {
                path: '/',
                element: <Navigate to="/home" replace />,
            },
            {
                path: '/home',
                element: <Home />,
            },
            {
                path: '/login',
                element: <Login />,
            },
            {
                path: '/signup',
                element: <Signup />
            },
            {
                path: '/lobbies',
                element: <Lobbies />
            },
            {
                path: '/lobbies/:lobbyId',
                element: <LobbyDetails />
            }
        ]
    }
]);

export default router;
