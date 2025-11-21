import { useEffect, useState } from 'react';
import { lobbyService } from '../../services/lobbyService';
import { isOk } from '../../services/utils';

interface Lobby {
    id: number;
    name: string;
    hostName: string;
    currentPlayers: number;
    maxPlayers: number;
    status: string;
    createdAt: string;
}

export function Lobbies() {
    const [lobbies, setLobbies] = useState<Lobby[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchLobbies();
    }, []);

    const fetchLobbies = async () => {
        setLoading(true);
        const result = await lobbyService.getAvailableLobbies();

        if (isOk(result)) {
            setLobbies(result.value);
            setError(null);
        } else {
            setError(result.error || 'Could not load lobbies. Please try again.');
        }

        setLoading(false);
    };

    const filteredLobbies = lobbies.filter((lobby) =>
        lobby.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-xl">Loading lobbies...</div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-100 p-8">
            <div className="max-w-6xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold">Public Lobbies</h1>
                </div>

                <div className="mb-6">
                    <input
                        type="text"
                        placeholder="Search lobbies..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
                        {error}
                    </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredLobbies.length === 0 ? (
                        <div className="col-span-full text-center py-12 text-gray-500">
                            No lobbies found.
                        </div>
                    ) : (
                        filteredLobbies.map((lobby) => (
                            <div
                                key={lobby.id}
                                className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow"
                            >
                                <h3 className="text-xl font-semibold mb-2">{lobby.name}</h3>
                                <div className="space-y-2 text-gray-600 mb-4">
                                    <p>Host: {lobby.hostName}</p>
                                    <p>
                                        Players: {lobby.currentPlayers}/{lobby.maxPlayers}
                                    </p>
                                    <p className="text-sm">Status: {lobby.status}</p>
                                </div>
                            </div>
                        ))
                    )}
                </div>

                <div className="mt-6 text-center">
                    <button
                        onClick={fetchLobbies}
                        className="text-blue-600 hover:text-blue-800 underline"
                    >
                        Refresh Lobbies
                    </button>
                </div>
            </div>
        </div>
    );
}
