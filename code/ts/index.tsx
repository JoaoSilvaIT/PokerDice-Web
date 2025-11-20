import * as React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

// Assumindo que tens um index.html com <div id="root"></div>
const container = document.getElementById('root');
if (container) {
    const root = createRoot(container);
    root.render(
        <React.StrictMode>
            <App />
        </React.StrictMode>
    );
}