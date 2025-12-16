import React, {useEffect, useState, useCallback} from 'react';
import {useNavigate} from 'react-router-dom';
import {lobbyService, Lobby} from '../../services/lobbyService';
import {isOk, formatError} from '../../services/utils';
import {useSSE} from '../../providers/SSEContext';
import {ToastContainer, useToast} from '../generic/toast';
import '../../styles/lobbies.css';
import { CreateLobbyMenu } from './createLobbyMenu';

export function Lobbies() {
    const navigate = useNavigate();
    const {connectToAllLobbies, registerAllLobbiesHandler, disconnect} = useSSE();
    const [lobbies, setLobbies] = useState<Lobby[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [showCreateMenu, setShowCreateMenu] = useState(false);
    const [isCreating, setIsCreating] = useState(false);
    const [joiningLobbyId, setJoiningLobbyId] = useState<number | null>(null);
    const {toasts, removeToast, showError} = useToast();

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
            disconnect('all-lobbies');
        };
    }, [fetchLobbies, connectToAllLobbies, registerAllLobbiesHandler, disconnect]);

    const handleCreateLobby = async (formData: { name: string; description: string; minPlayers: number; maxPlayers: number }) => {
        setIsCreating(true);

        const result = await lobbyService.createLobby(formData);

        if (isOk(result)) {
            setShowCreateMenu(false);
            navigate(`/lobbies/${result.value.id}`);
        } else {
            if (result.error?.includes('Unauthorized') || result.error?.includes('401')) {
                showError('You must be logged in to create a lobby');
                setTimeout(() => {
                    navigate('/login', {state: {source: '/lobbies'}});
                }, 2000);
            } else {
                showError(formatError(result.error || 'Failed to create lobby'));
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
                showError('You must be logged in to join a lobby');
                setTimeout(() => {
                    navigate('/login', {state: {source: '/lobbies'}});
                }, 2000);
            } else {
                showError(formatError(result.error || 'Failed to join lobby'));
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
            <ToastContainer toasts={toasts} removeToast={removeToast} />
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

            <CreateLobbyMenu 
                isOpen={showCreateMenu} 
                onClose={() => setShowCreateMenu(false)} 
                onCreate={handleCreateLobby} 
                isCreating={isCreating} 
            />
        </div>
    );
}
