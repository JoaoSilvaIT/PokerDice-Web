export type Result<T> =
    | { success: true; value: T }
    | { success: false; error: string };

export const isOk = <T>(result: Result<T>): result is { success: true; value: T } => result.success;

export async function fetchWrapper<T>(
    url: string,
    options: RequestInit = {}
): Promise<Result<T>> {
    try {
        const headers: HeadersInit = {
            'Content-Type': 'application/json',
            ...options.headers,
        };

        const response = await fetch(url, {
            ...options,
            credentials: 'include',
            headers,
        });

        if (!response.ok) {
            // Auto-logout on invalid session (401)
            if (response.status === 401) {
                if (!window.location.pathname.endsWith('/login')) {
                    localStorage.removeItem('username');
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userEmail');
                    document.cookie = 'token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
                    // Let the application handle the redirect via state change or router
                    return {success: false, error: 'Session expired'};
                }
            }

            const clonedResponse = response.clone();

            let errorMessage = 'Request failed';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorData.error || JSON.stringify(errorData);
            } catch (e) {
                try {
                    const errorText = await clonedResponse.text();
                    errorMessage = errorText || `HTTP ${response.status}`;
                } catch (textError) {
                    errorMessage = `HTTP ${response.status}`;
                }
            }

            return {success: false, error: errorMessage};
        }
        if (response.status === 204) {
            return {success: true, value: undefined as T};
        }

        const data = await response.json();
        return {success: true, value: data as T};
    } catch (error) {
        console.error('fetchWrapper error:', error);
        return {success: false, error: (error as Error).message};
    }
}

export const formatError = (err: string | object, errorMap?: Record<string, string>): string => {
    try {
        const parsed = typeof err === 'string' ? JSON.parse(err) : err;

        const msg = parsed.title || parsed.detail || parsed.message || parsed.error;
        if (msg && typeof msg === 'string') {
            const lowerCaseMsg = msg.toLowerCase();
            // Check against specific error map first
            if (errorMap) {
                const errorKey = Object.keys(errorMap).find(key => lowerCaseMsg.includes(key));
                if (errorKey) {
                    return errorMap[errorKey];
                }
            }

            if (lowerCaseMsg.includes('session expired') || lowerCaseMsg.includes('unauthorized')) {
                return 'Your session has expired. Please log in again.';
            }

            if (lowerCaseMsg.startsWith('http') || lowerCaseMsg.includes('urn:')) {
                const parts = lowerCaseMsg.split('/');
                return parts[parts.length - 1].replace(/-/g, ' ');
            }
            return msg
                .replace(/-/g, ' ')
                .replace(/([A-Z])/g, ' $1')
                .toLowerCase()
                .replace(/^\w/, (c: string) => c.toUpperCase())
                .trim();
        }
    } catch {
        // Fallback for non-JSON strings
    }

    if (typeof err === 'string') {
        if (err.toLowerCase().includes('session expired') || err.toLowerCase().includes('unauthorized')) {
            return 'Your session has expired. Please log in again.';
        }
        if (err.startsWith('http') || err.includes('urn:')) {
            const parts = err.split('/');
            return parts[parts.length - 1].replace(/-/g, ' ');
        }
        return err;
    }
    return 'An unknown error occurred';
};