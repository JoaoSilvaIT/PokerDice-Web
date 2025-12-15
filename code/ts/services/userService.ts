import {RequestUri} from './requestUri';
import {fetchWrapper, Result} from './utils';

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
        const result = await fetchWrapper<{ newBalance: number }>('/api/users/easteregg', {
            method: 'POST',
            credentials: 'include',
        });
        if (result.success) {
            return {success: true, value: result.value.newBalance};
        }
        return {success: false, error: result.error};
    },
};