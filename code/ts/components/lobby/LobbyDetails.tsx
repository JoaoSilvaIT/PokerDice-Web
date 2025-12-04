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
    const [showGameConfigMenu, setShowGameConfigMenu] = useState(false);
    const [gameConfig, setGameConfig] = useState({
        rounds: 5,
    });
    const [configError, setConfigError] = useState<string | null>(null);

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

        registerLobbyHandler(
            lobbyIdNum,
            (event) => {
                console.log('Player joined:', event.playerName);
                fetchLobbyDetails();
            },
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

    const handleGameConfigChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setGameConfig(prev => ({
            ...prev,
            [name]: name === 'rounds' ? parseInt(value) || 1 : value,
        }));
    };

    const handleStartGame = async () => {
        // Validate configuration
        if (gameConfig.rounds < 1 || gameConfig.rounds > 20) {
            setConfigError('Rounds must be between 1 and 20');
            return;
        }

        // TODO: Implement game start logic with gameConfig
        console.log('Starting game with config:', gameConfig);
        setShowGameConfigMenu(false);
    };


    const handleLeaveLobby = async () => {
        if (!lobbyId) return;

        const result = await lobbyService.leaveLobby(parseInt(lobbyId));

        if (isOk(result)) {
            disconnect();
            navigate('/lobbies');
        } else {
            setError(result.error || 'Failed to leave lobby. Please try again.');
        }
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
    const currentUserId = parseInt(localStorage.getItem('userId') || '0');
    const isHost = currentUserId === lobby.hostId;


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
                    <div className="lobby-footer-buttons">
                        {isHost && (
                            <button
                                onClick={() => setShowGameConfigMenu(true)}
                                className="configure-game-button"
                            >
                                Game Settings
                            </button>
                        )}
                        <button onClick={handleLeaveLobby} className="leave-lobby-button">
                            Leave Lobby
                        </button>
                    </div>
                </div>
            </div>

            {/* Overlay */}
            {showGameConfigMenu && (
                <div
                    className="lobby-menu-overlay"
                    onClick={() => setShowGameConfigMenu(false)}
                />
            )}

            {/* Side Menu for Game Configuration */}
            <div className={`lobby-create-menu ${showGameConfigMenu ? 'open' : ''}`}>
                <div className="lobby-create-menu-header">
                    <h2 className="lobby-create-menu-title">Game Configuration</h2>
                    <button
                        onClick={() => {
                            setShowGameConfigMenu(false);
                            setConfigError(null);
                        }}
                        className="lobby-create-menu-close"
                    >
                        ✕
                    </button>
                </div>

                <div className="lobby-create-form">
                    <div className="lobby-form-group">
                        <label htmlFor="rounds" className="lobby-form-label">
                            Number of Rounds
                        </label>
                        <input
                            type="number"
                            id="rounds"
                            name="rounds"
                            value={gameConfig.rounds}
                            onChange={handleGameConfigChange}
                            min="1"
                            max="20"
                            className="lobby-form-input"
                        />
                    </div>

                    <div className="lobby-form-hint">
                        Configure how many rounds the game will have (1-20)
                    </div>

                    {configError && <div className="lobby-create-error">{configError}</div>}

                    <div className="lobby-create-actions">
                        <button
                            onClick={handleStartGame}
                            className="lobby-create-button"
                            disabled={!canStartGame}
                        >
                            {canStartGame ? 'Start Game' : `Need ${lobby.minPlayers - lobby.players.length} more player(s)`}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
