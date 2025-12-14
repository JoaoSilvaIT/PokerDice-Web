import {RequestUri} from './requestUri';
import {fetchWrapper, Result} from './utils';

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
    rollsLeft: number;
    currentDice: string[];
    pot: number;
    winners?: PlayerInGame[];
}

export interface PlayerInGame {
    id: number;
    name: string;
    currentBalance: number;
    moneyWon: number;
}

export interface GameDetails {
    id: number;
    startedAt: number;
    endedAt: number | null;
    lobbyId: number;
    numberOfRounds: number;
    state: string;
    currentRound: GameRound | null;
    players: PlayerInGame[];
}

export interface RolledDice {
    dice: string[];
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

    async startGame(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(`/api/games/${gameId}/start`, {
            method: 'POST',
        });
    },

    async rollDices(gameId: number): Promise<Result<RolledDice>> {
        return await fetchWrapper<RolledDice>(`/api/games/${gameId}/rounds/roll-dices`, {
            method: 'POST',
        });
    },

    async updateTurn(gameId: number, dice: string): Promise<Result<RolledDice>> {
        return await fetchWrapper<RolledDice>(`/api/games/${gameId}/rounds/update-turn`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({dice}),
        });
    },

    async nextTurn(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(`/api/games/${gameId}/rounds/next-turn`, {
            method: 'POST',
        });
    },

    async startRound(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(`/api/games/${gameId}/rounds/start`, {
            method: 'POST',
        });
    },

    async setAnte(gameId: number, ante: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(`/api/games/${gameId}/rounds/ante`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ante}),
        });
    },

    async payAnte(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(`/api/games/${gameId}/rounds/pay-ante`, {
            method: 'POST',
        });
    }
}