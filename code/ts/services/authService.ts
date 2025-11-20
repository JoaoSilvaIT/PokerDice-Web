import { RequestUri } from './requestUri';
import {fetchWrapper, Result} from "./utils";

interface LoginCredentials {
    username: string;
    password: string;
}

export const authService = {
    login(credentials: LoginCredentials): Promise<Result<any>> {
        return fetchWrapper(RequestUri.user.login, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    },

    signup(credentials: LoginCredentials): Promise<Result<any>> {
        return fetchWrapper(RequestUri.user.signup, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    },
};