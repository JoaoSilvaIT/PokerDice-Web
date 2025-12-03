import {RequestUri} from './requestUri';
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

        return result;
    },

    async signup(credentials: SignupCredentials): Promise<Result<AuthResponse>> {
        const signupResult = await fetchWrapper<SignupResponse>(RequestUri.user.signup, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });

        if (!signupResult.success) {
            return signupResult as Result<AuthResponse>;
        }

        return await this.login({
            email: credentials.email,
            password: credentials.password
        });
    },

    async logout(): Promise<Result<void>> {
        const result = await fetchWrapper<void>(RequestUri.user.logout, {
            method: 'POST',
            credentials: 'include',
        });

        return result;
    }
};