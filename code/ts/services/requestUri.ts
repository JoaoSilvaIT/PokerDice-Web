const BASE_API_URL = '/api'

export const RequestUri = {
    user: {
        login: `${BASE_API_URL}/users/token`,
        signup: `${BASE_API_URL}/users`,
        stats: `${BASE_API_URL}/users/stats`,
        searchUsers: `${BASE_API_URL}/users/search?query=`,
    },
    game: {
        create: `${BASE_API_URL}/games/create`,
        list: `${BASE_API_URL}/games`,
        join: (gameId: number) => `${BASE_API_URL}/games/${gameId}/join`,
        leave: (gameId: number) => `${BASE_API_URL}/games/${gameId}/leave`,
        details: (gameId: number) => `${BASE_API_URL}/games/${gameId}`,
        roll: (gameId: number) => `${BASE_API_URL}/games/${gameId}/roll`,
        hold: (gameId: number) => `${BASE_API_URL}/games/${gameId}/hold`,
        end: (gameId: number) => `${BASE_API_URL}/games/${gameId}/end`,
        state: (gameId: number) => `${BASE_API_URL}/games/${gameId}/state`,
    },
    lobby: {
        list: `${BASE_API_URL}/lobbies`,
        create: `${BASE_API_URL}/lobbies`,
        join: (lobbyId: number) => `${BASE_API_URL}/lobbies/${lobbyId}/join`,
        leave: (lobbyId: number) => `${BASE_API_URL}/lobbies/${lobbyId}/leave`,
        close: (lobbyId: number) => `${BASE_API_URL}/lobbies/${lobbyId}`,
    },
    leaderboard: {
        global: `${BASE_API_URL}/leaderboard`,
        daily: `${BASE_API_URL}/leaderboard/daily`,
        weekly: `${BASE_API_URL}/leaderboard/weekly`,
        friends: `${BASE_API_URL}/leaderboard/friends`,
    },
    notifications: {
        getInvites: `${BASE_API_URL}/users/invites`,
        acceptInvite: (inviteId: number) => `${BASE_API_URL}/invites/${inviteId}/accept`,
        declineInvite: (inviteId: number) => `${BASE_API_URL}/invites/${inviteId}/decline`,
    }
}
