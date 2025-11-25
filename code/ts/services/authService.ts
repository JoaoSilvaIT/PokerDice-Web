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

interface UserMeResponse {
    id: number;
    name: string;
    email: string;
}

export const authService = {
    async login(credentials: LoginCredentials): Promise<Result<AuthResponse>> {
        // Backend sets the cookie via Set-Cookie header automatically
        return await fetchWrapper<AuthResponse>(RequestUri.user.login, {
            method: 'POST',
            body: JSON.stringify(credentials),
            credentials: 'include', // Important: allows cookies to be set/sent
        });
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

    async logout(): Promise<Result<void>> {
        return await fetchWrapper<void>(RequestUri.user.logout, {
            method: 'POST',
            credentials: 'include',
        });
    },
};