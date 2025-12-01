import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { lobbyService } from '../../services/lobbyService';
import { isOk } from '../../services/utils';
import { useSSE } from '../../providers/SSEContext';
import '../../styles/lobbyDetails.css';

interface Player {
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

export function LobbyDetails() {
    const { lobbyId } = useParams<{ lobbyId: string }>();
    const navigate = useNavigate();
    const [lobby, setLobby] = useState<LobbyDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const { connectToLobby, disconnect, isConnected, registerLobbyHandler, unregisterHandler } = useSSE();

    const fetchLobbyDetails = async () => {
        if (!lobbyId) return;

        const result = await lobbyService.getLobbyDetails(parseInt(lobbyId));

        if (isOk(result)) {
            setLobby(result.value);
            setError(null);
        } else {
            setError(result.error || 'Could not load lobby details. Please try again.');
        }

        setLoading(false);
    };

    useEffect(() => {
        if (!lobbyId) return;

        const lobbyIdNum = parseInt(lobbyId);

        fetchLobbyDetails();

        // Register SSE handlers
        registerLobbyHandler(
            lobbyIdNum,
            (event) => {
                console.log('Player joined:', event.playerName);
                fetchLobbyDetails();
            },
            // On player left
            (event) => {
                console.log('Player left:', event.playerId);
                fetchLobbyDetails();
            }
        );

        connectToLobby(lobbyIdNum).catch((error) => {
            console.error('Failed to connect to SSE:', error);
        });

        return () => {
            unregisterHandler();
            disconnect();
        };
    }, [lobbyId]);

    const handleStartGame = async () => {
        // TODO: Implement game start logic
        console.log('Starting game...');
    };

    if (loading) {
        return (
            <div className="lobby-details-container">
                <div className="lobby-details-loading">Loading lobby...</div>
            </div>
        );
    }

    if (error || !lobby) {
        return (
            <div className="lobby-details-container">
                <div className="lobby-details-error">
                    {error || 'Lobby not found'}
                </div>
                <button onClick={() => navigate('/lobbies')} className="back-button">
                    Back to Lobbies
                </button>
            </div>
        );
    }

    const canStartGame = lobby.players.length >= lobby.minPlayers;

    return (
        <div className="lobby-details-container">
            <div className="lobby-details-content">
                <div className="lobby-details-header">
                    <h1 className="lobby-title">{lobby.name}</h1>
                    {lobby.description && (
                        <p className="lobby-description">{lobby.description}</p>
                    )}
                </div>

                <div className="players-section">
                    <h2 className="players-title">Players ({lobby.players.length}/{lobby.maxPlayers})</h2>
                    <div className="players-list">
                        {lobby.players?.map((player) => (
                            <div key={player.id} className="player-bar">
                                <span className="player-name">
                                    {player.name || 'Unknown'}
                                    {player.id === lobby.hostId && ' (Host)'}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="lobby-footer">
                    <span className="update-indicator">
                        {isConnected ? '● Live' : '○ Connecting...'}
                    </span>
                </div>
            </div>
        </div>
    );
}
