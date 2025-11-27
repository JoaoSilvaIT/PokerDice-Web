import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { lobbyService } from '../../services/lobbyService';
import { isOk } from '../../services/utils';
import '../../styles/lobbyDetails.css';

interface Player {
    id: number;
    username: string;
}

interface LobbyDetails {
    id: number;
    name: string;
    description: string;
    hostName: string;
    currentPlayers: number;
    maxPlayers: number;
    minPlayers: number;
    createdAt: string;
    players: Player[];
}

export function LobbyDetails() {
    const { lobbyId } = useParams<{ lobbyId: string }>();
    const navigate = useNavigate();
    const [lobby, setLobby] = useState<LobbyDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

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
        fetchLobbyDetails();

        // Poll for updates every 3 seconds
        const intervalId = setInterval(() => {
            fetchLobbyDetails();
        }, 3000);

        // Cleanup interval on component unmount
        return () => clearInterval(intervalId);
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

    const canStartGame = lobby.currentPlayers >= lobby.minPlayers;

    return (
        <div className="lobby-details-container">
            <div className="lobby-details-content">
                <div className="lobby-details-header">
                    <h1 className="lobby-title">{lobby.name}</h1>
                </div>

                {lobby.description && (
                    <p className="lobby-description">{lobby.description}</p>
                )}

                <div className="lobby-info">
                    <div className="info-item">
                        <span className="info-label">Host:</span>
                        <span className="info-value">{lobby.hostName}</span>
                    </div>
                    <div className="info-item">
                        <span className="info-label">Players:</span>
                        <span className="info-value">
                            {lobby.currentPlayers}/{lobby.maxPlayers}
                        </span>
                    </div>
                    <div className="info-item">
                        <span className="info-label">Min Players:</span>
                        <span className="info-value">{lobby.minPlayers}</span>
                    </div>
                </div>

                <div className="players-section">
                    <h2 className="players-title">Players ({lobby.currentPlayers})</h2>
                    <div className="players-list">
                        {lobby.players?.map((player) => (
                            <div key={player.id} className="player-card">
                                <div className="player-avatar">
                                    {player.username?.charAt(0).toUpperCase() || '?'}
                                </div>
                                <div className="player-info">
                                    <div className="player-name">{player.username || 'Unknown'}</div>

                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="lobby-footer">
                    <span className="update-indicator">Auto-updating...</span>
                </div>
            </div>
        </div>
    );
}
