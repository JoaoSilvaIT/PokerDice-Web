import { RequestUri } from './requestUri';
import {fetchWrapper, Result} from "./utils";

interface LoginCredentials {
    email: string;
    password: string;
}

interface SignupCredentials {
    name: string;
    email: string;
    password: string;
    invite: string;
}

interface AuthResponse {
    token: string;
}

interface SignupResponse {
    name: string;
    balance: number;
}

export const authService = {
    async login(credentials: LoginCredentials): Promise<Result<AuthResponse>> {
        const result = await fetchWrapper<AuthResponse>(RequestUri.user.login, {
            method: 'POST',
            body: JSON.stringify(credentials),
            credentials: 'include',
        });

        // Store the token in localStorage if login was successful
        if (result.success && result.value.token) {
            localStorage.setItem('authToken', result.value.token);
            console.log('Token saved to localStorage');
        }

        return result;
    },

    async signup(credentials: SignupCredentials): Promise<Result<AuthResponse>> {
        // Signup endpoint doesn't return a token, so we need to login after
        const signupResult = await fetchWrapper<SignupResponse>(RequestUri.user.signup, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });

        if (!signupResult.success) {
            return signupResult as Result<AuthResponse>;
        }

        // After successful signup, automatically login to get the token (and cookie)
        return await this.login({
            email: credentials.email,
            password: credentials.password
        });
    },

    logout() {
        localStorage.removeItem('authToken');
        console.log('Token removed from localStorage');
    }
};