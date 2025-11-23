import { useEffect, useState } from 'react';
import { lobbyService } from '../../services/lobbyService';
import { isOk } from '../../services/utils';
import '../../styles/lobbies.css';

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
        <div className="lobbies-container">
            <div className="lobbies-content">
                <div className="lobbies-header">
                    <h1 className="lobbies-title">Public Lobbies</h1>
                </div>

                <div className="lobbies-search">
                    <input
                        type="text"
                        placeholder="Search lobbies..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="search-input"
                    />
                </div>

                {error && <div className="lobbies-error">{error}</div>}

                <div className="lobbies-grid">
                    {filteredLobbies.length === 0 ? (
                        <div className="lobbies-empty">No lobbies found.</div>
                    ) : (
                        filteredLobbies.map((lobby) => (
                            <div key={lobby.id} className="lobby-card">
                                <div className="lobby-name">{lobby.name}</div>
                                <div className="lobby-players">
                                    {lobby.currentPlayers}/{lobby.maxPlayers}
                                </div>
                            </div>
                        ))
                    )}
                </div>

                <div className="lobbies-footer">
                    <button
                        onClick={fetchLobbies}
                        className="refresh-button"
                    >
                        Refresh Lobbies
                    </button>
                </div>
            </div>
        </div>
    );
}
