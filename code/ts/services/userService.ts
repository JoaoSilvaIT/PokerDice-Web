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
};