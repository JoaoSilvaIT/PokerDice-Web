import * as React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import {Navbar} from "./components/generic/navbar";
import './styles/global.css';

const container = document.getElementById('root');
if (container) {
    const root = createRoot(container);
    root.render(
        <React.StrictMode>
            <Navbar />
            <App />
        </React.StrictMode>
    );
}