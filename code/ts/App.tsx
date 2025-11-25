import * as React from 'react';
import { RouterProvider } from 'react-router-dom';
import router from './routes';
import { AuthenticationProvider } from './providers/authentication';

function App() {
    return (
        <AuthenticationProvider>
            <RouterProvider router={router} />
        </AuthenticationProvider>
    );
}

export default App;