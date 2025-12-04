export type Result<T> =
    | { success: true; value: T }
    | { success: false; error: string };

export const isOk = <T>(result: Result<T>): result is { success: true; value: T } => result.success;

export async function fetchWrapper<T>(
    url: string,
    options: RequestInit = {}
): Promise<Result<T>> {
    try {
        console.log('fetchWrapper URL:', url);
        console.log('fetchWrapper cookies:', document.cookie);

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
            console.log('fetchWrapper status:', response.status);

            const clonedResponse = response.clone();

            let errorMessage = 'Request failed';
            try {
                const errorData = await response.json();
                console.log('Error response data:', errorData);
                errorMessage = errorData.message || errorData.error || JSON.stringify(errorData);
            } catch (e) {
                try {
                    const errorText = await clonedResponse.text();
                    console.log('Error response text:', errorText);
                    errorMessage = errorText || `HTTP ${response.status}`;
                } catch (textError) {
                    errorMessage = `HTTP ${response.status}`;
                }
            }

            return { success: false, error: errorMessage };
        }
        if (response.status === 204) {
            return { success: true, value: undefined as T };
        }

        const data = await response.json();
        return { success: true, value: data as T };
    } catch (error) {
        console.error('fetchWrapper error:', error);
        return { success: false, error: error.message };
    }
}