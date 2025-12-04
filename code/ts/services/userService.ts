import { RequestUri } from './requestUri';
import { fetchWrapper, Result } from './utils';

export interface UserStatistics {
    gamesPlayed: number;
    wins: number;
    losses: number;
    winRate: number;
}

export const userService = {
    async getUserStats(): Promise<Result<UserStatistics>> {
        return await fetchWrapper<UserStatistics>(RequestUri.user.stats, {
            method: 'GET',
            credentials: 'include',
        });
    },
};
