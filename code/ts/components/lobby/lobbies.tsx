import React, {useEffect, useState, useCallback} from 'react';
import {useNavigate} from 'react-router-dom';
import {lobbyService, Lobby} from '../../services/lobbyService';
import {isOk} from '../../services/utils';
import {useSSE} from '../../providers/SSEContext';
import '../../styles/lobbies.css';

export function Lobbies() {
    const navigate = useNavigate();
    const {connectToAllLobbies, registerAllLobbiesHandler, disconnect} = useSSE();
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
    const [createError, setCreateError] = useState<string | null>(null);
    const [isCreating, setIsCreating] = useState(false);
    const [joiningLobbyId, setJoiningLobbyId] = useState<number | null>(null);

    const fetchLobbies = useCallback(async () => {
        setLoading(true);
        const result = await lobbyService.getAvailableLobbies();

        if (isOk(result)) {
            setLobbies(result.value as Lobby[]);
            setError(null);
        } else {
            setError(result.error || 'Could not load lobbies. Please try again.');
        }

        setLoading(false);
    }, []);

    useEffect(() => {
        fetchLobbies();

        const setupSSE = async () => {
            try {
                registerAllLobbiesHandler(
                    (event) => {
                        fetchLobbies();
                    },
                    (event) => {
                        fetchLobbies();
                    },
                    (event) => {
                        fetchLobbies();
                    }
                );

                await connectToAllLobbies();
            } catch (error) {
                console.error('[Lobbies] Failed to connect to SSE:', error);
            }
        };

        setupSSE();

        return () => {
            disconnect();
        };
    }, [fetchLobbies, connectToAllLobbies, registerAllLobbiesHandler, disconnect]);

    const handleCreateFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const {name, value} = e.target;
        setCreateFormData(prev => ({
            ...prev,
            [name]: name === 'minPlayers' || name === 'maxPlayers' ? parseInt(value) || 0 : value,
        }));
    };

    const handleCreateLobby = async () => {
        if (!createFormData.name.trim()) {
            setCreateError('Lobby name is required');
            return;
        }

        if (createFormData.minPlayers < 2 || createFormData.minPlayers > 10) {
            setCreateError('Min players must be between 2 and 10');
            return;
        }

        if (createFormData.maxPlayers < 2 || createFormData.maxPlayers > 10) {
            setCreateError('Max players must be between 2 and 10');
            return;
        }

        if (createFormData.minPlayers > createFormData.maxPlayers) {
            setCreateError('Min players cannot be greater than max players');
            return;
        }

        setIsCreating(true);
        setCreateError(null);

        const result = await lobbyService.createLobby(createFormData);

        if (isOk(result)) {
            setCreateFormData({name: '', description: '', minPlayers: 2, maxPlayers: 4});
            setShowCreateMenu(false);
            setCreateError(null);

            navigate(`/lobbies/${result.value.id}`);
        } else {
            if (result.error?.includes('Unauthorized') || result.error?.includes('401')) {
                setCreateError('You must be logged in to create a lobby');
                setTimeout(() => {
                    navigate('/login', {state: {source: '/lobbies'}});
                }, 2000);
            } else {
                setCreateError(result.error || 'Failed to create lobby. Please try again.');
            }
        }

        setIsCreating(false);
    };

    const handleJoinLobby = async (lobbyId: number, e: React.MouseEvent) => {
        e.stopPropagation();

        setJoiningLobbyId(lobbyId);

        const result = await lobbyService.joinLobby(lobbyId);

        if (isOk(result)) {
            navigate(`/lobbies/${lobbyId}`);
        } else {
            if (result.error?.includes('Unauthorized') || result.error?.includes('401')) {
                setError('You must be logged in to join a lobby');
                setTimeout(() => {
                    navigate('/login', {state: {source: '/lobbies'}});
                }, 2000);
            } else {
                setError(result.error || 'Failed to join lobby. Please try again.');
            }
        }

        setJoiningLobbyId(null);
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
                    <button
                        onClick={() => setShowCreateMenu(true)}
                        className="create-lobby-button"
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
                            <div
                                key={lobby.id}
                                className="lobby-card"
                                onClick={() => navigate(`/lobbies/${lobby.id}`)}
                            >
                                <div className="lobby-card-content">
                                    <div className="lobby-name">{lobby.name}</div>
                                    <div className="lobby-info">
                                        <div
                                            className="lobby-host">Host: {lobby.players.find(p => p.id === lobby.hostId)?.name}</div>
                                        <div className="lobby-players">
                                            {lobby.players.length}/{lobby.maxPlayers} Players
                                        </div>
                                    </div>
                                </div>
                                <button
                                    className="join-lobby-button"
                                    onClick={(e) => handleJoinLobby(lobby.id, e)}
                                    disabled={joiningLobbyId === lobby.id || lobby.players.length >= lobby.maxPlayers}
                                >
                                    {joiningLobbyId === lobby.id
                                        ? 'Joining...'
                                        : lobby.players.length >= lobby.maxPlayers
                                            ? 'Full'
                                            : 'Join'}
                                </button>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {showCreateMenu && (
                <div
                    className="lobby-menu-overlay"
                    onClick={() => setShowCreateMenu(false)}
                />
            )}

            <div className={`lobby-create-menu ${showCreateMenu ? 'open' : ''}`}>
                <div className="lobby-create-menu-header">
                    <h2 className="lobby-create-menu-title">Create New Lobby</h2>
                    <button
                        onClick={() => {
                            setShowCreateMenu(false);
                            setCreateFormData({name: '', description: '', minPlayers: 2, maxPlayers: 4});
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

                    {createError && <div className="lobby-create-error">{createError}</div>}

                    <div className="lobby-create-actions">
                        <button
                            onClick={handleCreateLobby}
                            className="lobby-create-button"
                            disabled={isCreating}
                        >
                            {isCreating ? 'Creating...' : 'Create Lobby'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
