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
    const [showCreateMenu, setShowCreateMenu] = useState(false);
    const [createFormData, setCreateFormData] = useState({
        name: '',
        description: '',
        minPlayers: 2,
        maxPlayers: 4,
    });

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

    const handleCreateFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setCreateFormData(prev => ({
            ...prev,
            [name]: name === 'minPlayers' || name === 'maxPlayers' ? parseInt(value) || 0 : value,
        }));
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
        <div style={{ position: 'relative', minHeight: '100vh', overflow: 'hidden' }}>
            <div className="lobbies-container">
                <div className="lobbies-content">
                    <div className="lobbies-header">
                        <h1 className="lobbies-title">Public Lobbies</h1>
                        <button
                            onClick={() => setShowCreateMenu(true)}
                            className="create-lobby-button"
                            style={{
                                padding: '10px 20px',
                                backgroundColor: '#8b5cf6',
                                color: 'white',
                                border: 'none',
                                borderRadius: '8px',
                                fontSize: '16px',
                                fontWeight: 'bold',
                                cursor: 'pointer',
                                transition: 'background-color 0.2s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#7c3aed'}
                            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#8b5cf6'}
                        >
                            + Create Lobby
                        </button>
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

            {/* Overlay */}
            {showCreateMenu && (
                <div
                    className="lobby-menu-overlay"
                    onClick={() => setShowCreateMenu(false)}
                />
            )}

            {/* Side Menu */}
            <div className={`lobby-create-menu ${showCreateMenu ? 'open' : ''}`}>
                <div className="lobby-create-menu-header">
                    <h2 className="lobby-create-menu-title">Create New Lobby</h2>
                    <button
                        onClick={() => {
                            setShowCreateMenu(false);
                            setCreateFormData({ name: '', description: '', minPlayers: 2, maxPlayers: 4 });
                        }}
                        className="lobby-create-menu-close"
                    >
                        ✕
                    </button>
                </div>

                <div className="lobby-create-form">
                    <div className="lobby-form-group">
                        <label htmlFor="name" className="lobby-form-label">
                            Lobby Name *
                        </label>
                        <textarea
                            id="name"
                            name="name"
                            value={createFormData.name}
                            onChange={handleCreateFormChange}
                            rows={1}
                            className="lobby-form-textarea"
                            placeholder="Enter lobby name"
                        />
                    </div>

                    <div className="lobby-form-group">
                        <label htmlFor="description" className="lobby-form-label">
                            Description
                        </label>
                        <textarea
                            id="description"
                            name="description"
                            value={createFormData.description}
                            onChange={handleCreateFormChange}
                            rows={4}
                            className="lobby-form-textarea"
                            placeholder="Optional description"
                        />
                    </div>

                    <div className="lobby-form-group">
                        <label htmlFor="minPlayers" className="lobby-form-label">
                            Min Players
                        </label>
                        <input
                            type="number"
                            id="minPlayers"
                            name="minPlayers"
                            value={createFormData.minPlayers}
                            onChange={handleCreateFormChange}
                            min="2"
                            max="10"
                            className="lobby-form-input"
                        />
                    </div>

                    <div className="lobby-form-group">
                        <label htmlFor="maxPlayers" className="lobby-form-label">
                            Max Players
                        </label>
                        <input
                            type="number"
                            id="maxPlayers"
                            name="maxPlayers"
                            value={createFormData.maxPlayers}
                            onChange={handleCreateFormChange}
                            min="2"
                            max="10"
                            className="lobby-form-input"
                        />
                    </div>

                    <div className="lobby-form-hint">
                        Players must be between 2 and 10
                    </div>
                </div>
            </div>
        </div>
    );
}
