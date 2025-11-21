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

export const authService = {
    login(credentials: LoginCredentials): Promise<Result<any>> {
        return fetchWrapper(RequestUri.user.login, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    },

    signup(credentials: SignupCredentials): Promise<Result<any>> {
        return fetchWrapper(RequestUri.user.signup, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    },
};