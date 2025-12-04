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

interface UserInfo {
    id: number;
    name: string;
    email: string;
}

interface SignupResponse {
    name: string;
    balance: number;
}

export const authService = {
    async login(credentials: LoginCredentials): Promise<Result<AuthResponse>> {
        return await fetchWrapper<AuthResponse>(RequestUri.user.login, {
            method: 'POST',
            body: JSON.stringify(credentials),
            credentials: 'include',
        });
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
        return await fetchWrapper<void>(RequestUri.user.logout, {
            method: 'POST',
            credentials: 'include',
        });
    },

    async getUserInfo(): Promise<Result<UserInfo>> {
        return await fetchWrapper<UserInfo>(RequestUri.user.info, {
            method: 'GET',
            credentials: 'include',
        });
    },
};
