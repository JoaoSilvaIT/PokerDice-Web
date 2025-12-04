import { RequestUri } from './requestUri';
import { fetchWrapper, Result } from './utils';

interface CreateGameRequest {
    lobbyId: number;
    numberOfRounds: number;
}

interface CreateGameResponse {
    id: number;
}

export interface GameRound {
    number: number;
    ante: number;
    turnUserId: number;
}

export interface GameDetails {
    id: number;
    startedAt: number;
    endedAt: number | null;
    lobbyId: number;
    numberOfRounds: number;
    state: string;
    currentRound: GameRound | null;
}

export const gameService = {
    async createGame(data: CreateGameRequest): Promise<Result<CreateGameResponse>> {
        return await fetchWrapper<CreateGameResponse>(RequestUri.game.create, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
    },

    async getGame(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(`/api/games/${gameId}`, {
            method: 'GET',
        });
    },
}