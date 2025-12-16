import {RequestUri} from './requestUri';
import {fetchWrapper, isOk, Result} from './utils';

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
    handRank?: string;
}

export interface GameDetails {
    id: number;
    startedAt: number;
    endedAt: number | null;
    lobbyId: number | null;
    numberOfRounds: number;
    state: string;
    currentRound: GameRound | null;
    players: PlayerInGame[];
}

export interface RolledDice {
    dice: string[];
}


export interface RolledDiceOutputModel {
    dice: string[];
}

export interface DiceOutputModel {
    dices: string[];
}

export const gameService = {
    async createGame(data: CreateGameRequest): Promise<Result<CreateGameResponse>> {
        return await fetchWrapper<CreateGameResponse>(RequestUri.game.create, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
            credentials: 'include',
        });
    },

    async getGame(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(RequestUri.game.details(gameId), {
            method: 'GET',
            credentials: 'include',
        });
    },

    async startGame(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(RequestUri.game.start(gameId), {
            method: 'POST',
            credentials: 'include',
        });
    },

    async rollDices(gameId: number): Promise<Result<{ dice: string[] }>> {
        const result = await fetchWrapper<RolledDiceOutputModel>(RequestUri.game.roll(gameId), {
            method: 'POST',
            credentials: 'include',
        });
        if (isOk(result)) {
            return {success: true, value: {dice: result.value.dice}};
        }
        return {success: false, error: result.error};
    },

    async updateTurn(gameId: number, dice: string[]): Promise<Result<string[]>> {
        const result = await fetchWrapper<DiceOutputModel>(RequestUri.game.turn(gameId), {
            method: 'POST',
            body: JSON.stringify({dices: dice}), // Matches backend DiceUpdateInputModel
            credentials: 'include',
        });
        if (isOk(result)) {
            return {success: true, value: result.value.dices};
        }
        return {success: false, error: result.error};
    },

    async nextTurn(gameId: number): Promise<Result<GameDetails>> {
        const result = await fetchWrapper<GameDetails>(RequestUri.game.nextTurn(gameId), {
            method: 'POST',
            body: JSON.stringify({}),
            credentials: 'include',
        });
        return result;
    },

    async setAnte(gameId: number, ante: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(RequestUri.game.ante(gameId), {
            method: 'POST',
            body: JSON.stringify({ante}),
            credentials: 'include',
        });
    },

    async payAnte(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(RequestUri.game.payAnte(gameId), {
            method: 'POST',
            credentials: 'include',
        });
    },

    async startRound(gameId: number): Promise<Result<GameDetails>> {
        return await fetchWrapper<GameDetails>(RequestUri.game.startRound(gameId), {
            method: 'POST',
            body: JSON.stringify({}),
            credentials: 'include',
        });
    },
}