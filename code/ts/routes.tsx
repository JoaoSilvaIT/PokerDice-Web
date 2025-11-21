import * as React from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Home } from './components/generic/home';
import { Login } from './components/auth/login';
import {Signup} from "./components/auth/signup";
import { Lobbies } from './components/lobby/lobbies';

/**
 * Route definitions for PokerDice application
 * All possible routes are defined here
 */
export const router = createBrowserRouter([
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
    }
]);

export default router;
