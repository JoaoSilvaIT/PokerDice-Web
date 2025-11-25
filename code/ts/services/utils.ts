export type Result<T> =
    | { success: true; value: T }
    | { success: false; error: string };

export const isOk = <T>(result: Result<T>): result is { success: true; value: T } => result.success;

export async function fetchWrapper<T>(
    url: string,
    options: RequestInit = {}
): Promise<Result<T>> {
    try {
        console.log('fetchWrapper', document.cookie);
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            credentials: 'include',
            ...options,
        });

        if (!response.ok) {
            console.log('fetchWrapper', response.status);
            console.log('Response headers:', response.headers);
            const errorData = await response.json();
            return { success: false, error: errorData.message || 'Request failed' };
        }
        if (response.status === 204) {
            return { success: true, value: undefined as T };
        }

        const data = await response.json();
        return { success: true, value: data as T };
    } catch (error) {
        return { success: false, error: error.message };
    }
}