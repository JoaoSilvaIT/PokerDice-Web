import React, {useEffect, useState} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {lobbyService} from '../../services/lobbyService';
import {gameService} from '../../services/gameService';
import {isOk} from '../../services/utils';
import {useSSE} from '../../providers/SSEContext';
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
    const {lobbyId} = useParams<{ lobbyId: string }>();
    const navigate = useNavigate();
    const [lobby, setLobby] = useState<LobbyDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const {connectToLobby, disconnect, isConnected, registerLobbyHandler, unregisterHandler} = useSSE();
    const [showGameConfigMenu, setShowGameConfigMenu] = useState(false);
    const [gameConfig, setGameConfig] = useState({
        rounds: 5,
    });
    const [configError, setConfigError] = useState<string | null>(null);
    const [countdownEnd, setCountdownEnd] = useState<number | null>(null);
    const [timeLeft, setTimeLeft] = useState<number>(0);

    useEffect(() => {
        if (!countdownEnd) return;
        const interval = setInterval(() => {
            const remaining = countdownEnd - Date.now();
            setTimeLeft(remaining > 0 ? remaining : 0);
        }, 200);
        return () => clearInterval(interval);
    }, [countdownEnd]);

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
                fetchLobbyDetails();
            },
            (event) => {
                fetchLobbyDetails();
            },
            (event) => {
                disconnect('lobby');
                navigate(`/games/${event.gameId}`);
            },
            (event) => {                             // onCountdownStarted
                setCountdownEnd(event.expiresAt);
            },
            (event) => {                             // onCountdownCancelled
                setCountdownEnd(null);
            }
        );

        connectToLobby(lobbyIdNum)
            .then(() => {
                // Fetch again after SSE connection is established to catch any updates
                // that happened while connecting
                fetchLobbyDetails();
            })
            .catch((error) => {
                console.error('Failed to connect to SSE:', error);
            });

        return () => {
            unregisterHandler();
            disconnect('lobby');
        };
    }, [lobbyId]);

    const handleGameConfigChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setGameConfig(prev => ({
            ...prev,
            [name]: name === 'rounds' ? parseInt(value) || 1 : value,
        }));
    };
    const handleStartGame = async () => {
        if (!lobby || !lobbyId) return;

        setConfigError(null);

        const result = await gameService.createGame({
            lobbyId: parseInt(lobbyId),
            numberOfRounds: gameConfig.rounds
        });

        if (!isOk(result)) {
            setConfigError(result.error || 'Failed to create game');
        }
        // Game will start via SSE event which will navigate to the game page
    };

    const handleLeaveLobby = async () => {
        if (!lobbyId) return;

        const result = await lobbyService.leaveLobby(parseInt(lobbyId));

        if (isOk(result)) {
            disconnect('lobby');
            navigate('/lobbies');
        } else {
            setError(result.error || 'Failed to leave lobby. Please try again.');
        }
    };

    const handleDeleteLobby = async () => {
        if (!lobbyId) return;
        const result = await lobbyService.deleteLobby(parseInt(lobbyId));
        if (isOk(result)) {
            navigate('/lobbies');
        } else {
            setError(result.error || 'Failed to delete lobby.');
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

                    {timeLeft > 0 && (
                        <span className="lobby-countdown">
                        Game starts in {Math.ceil(timeLeft / 1000)}s
                        </span>
                    )}
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
