import { RequestUri } from './requestUri';
import { fetchWrapper, Result } from './utils';

interface Lobby {
    id: number;
    name: string;
    hostName: string;
    currentPlayers: number;
    maxPlayers: number;
    status: string;
    createdAt: string;
}

interface LobbyListResponse {
    lobbies: Lobby[];
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
};




