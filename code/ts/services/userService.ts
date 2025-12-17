import {RequestUri} from './requestUri';
import {fetchWrapper, isOk, Result} from './utils';

export interface UserStats {
    gamesPlayed: number;
    wins: number;
    losses: number;
    winRate: number;
}

export const userService = {
    async getUserStats(): Promise<Result<UserStats>> {
        return await fetchWrapper<UserStats>(RequestUri.user.stats, {
            method: 'GET',
            credentials: 'include',
        });
    },

    async claimEasterEgg(): Promise<Result<number>> {
        const result = await fetchWrapper<{ newBalance: number }>(RequestUri.user.easterEgg, {
            method: 'POST',
            credentials: 'include',
        });
        if (isOk(result)) {
            return {success: true, value: result.value.newBalance};
        }
        return {success: false, error: result.error};
    },

    async createInvite(): Promise<Result<string>> {
        const result = await fetchWrapper<{ code: string }>(RequestUri.user.createInvite, {
            method: 'POST',
            credentials: 'include',
        });
        if (isOk(result)) {
            return {success: true, value: result.value.code};
        }
        return {success: false, error: result.error};
    },
};