import React, { useState, useEffect, useCallback } from 'react';
import '../../styles/toast.css';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
    id: number;
    message: string;
    type: ToastType;
}

interface ToastProps {
    toasts: ToastMessage[];
    removeToast: (id: number) => void;
}

export const ToastContainer: React.FC<ToastProps> = ({ toasts, removeToast }) => {
    return (
        <div className="toast-container">
            {toasts.map((toast) => (
                <div key={toast.id} className={`toast ${toast.type}`}>
                    <span className="toast-message">{toast.message}</span>
                    <button className="toast-close" onClick={() => removeToast(toast.id)}>
                        ×
                    </button>
                </div>
            ))}
        </div>
    );
};

export function useToast() {
    const [toasts, setToasts] = useState<ToastMessage[]>([]);

    const addToast = useCallback((message: string, type: ToastType = 'info') => {
        const id = Date.now();
        setToasts((prev) => [...prev, { id, message, type }]);

        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id));
        }, 3000); // Auto dismiss after 3 seconds
    }, []);

    const removeToast = useCallback((id: number) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const showToast = (message: string, type: ToastType) => addToast(message, type);
    const showError = (message: string) => addToast(message, 'error');
    const showSuccess = (message: string) => addToast(message, 'success');

    return {
        toasts,
        removeToast,
        showToast,
        showError,
        showSuccess
    };
}
