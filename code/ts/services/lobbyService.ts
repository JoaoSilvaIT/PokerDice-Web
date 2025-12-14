import { RequestUri } from './requestUri';
import { fetchWrapper, Result } from './utils';

export interface Lobby {
    id: number;
    name: string;
    description?: string;
    minPlayers: number;
    maxPlayers: number;
    players: Player[];
    hostId: number;
}

export interface Player {
    id: number;
    name: string;
}

interface LobbyDetails {
    id: number;
    name: string;
    description: string;
    minPlayers: number;
    maxPlayers: number;
    players: Player[];
    hostId: number;
}

interface LobbyListResponse {
    lobbies: Lobby[];
}

interface CreateLobbyRequest {
    name: string;
    description: string;
    minPlayers: number;
    maxPlayers: number;
}

interface CreateLobbyResponse {
    id: number;
    name: string;
    description: string;
    hostName: string;
    currentPlayers: number;
    maxPlayers: number;
    minPlayers: number;
    status: string;
    createdAt: string;
}

export const lobbyService = {
    async getAvailableLobbies(): Promise<Result<Lobby[]>> {
        const result = await fetchWrapper<LobbyListResponse>(RequestUri.lobby.list, {
            method: 'GET',
        });

        if (!result.success) {
            return result as Result<Lobby[]>;
        }
        return { success: true, value: result.value.lobbies };
    },

    async createLobby(data: CreateLobbyRequest): Promise<Result<CreateLobbyResponse>> {
        return await fetchWrapper<CreateLobbyResponse>(RequestUri.lobby.create, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
    },

    async joinLobby(lobbyId: number): Promise<Result<void>> {
        return await fetchWrapper<void>(RequestUri.lobby.join(lobbyId), {
            method: 'POST',
        });
    },

    async getLobbyDetails(lobbyId: number): Promise<Result<LobbyDetails>> {
        return await fetchWrapper<LobbyDetails>(RequestUri.lobby.details(lobbyId), {
            method: 'GET',
        });
    }
}
