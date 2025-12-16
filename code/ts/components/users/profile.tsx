import * as React from 'react';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {authService} from '../../services/authService';
import {userService, UserStats} from '../../services/userService';
import {useAuthentication} from '../../providers/authentication';
import '../../styles/profile.css';

interface UserInfo {
    id: number;
    name: string;
    email: string;
    balance: number;
}

export function Profile() {
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [stats, setStats] = useState<UserStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [, , clearUsername] = useAuthentication();
    const navigate = useNavigate();

    useEffect(() => {
        const fetchData = async () => {
            try {
                const infoResult = await authService.getUserInfo();
                if (!infoResult.success) {
                    setError((infoResult as { success: false; error: string }).error ?? 'Failed to load user info');
                    return;
                }
                setUserInfo(infoResult.value as UserInfo);

                const statsResult = await userService.getUserStats();
                if (statsResult.success) {
                    setStats(statsResult.value);
                } else {
                    console.warn('Failed to load stats:', (statsResult as { success: false; error: string }).error);
                }
            } catch (err: any) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    async function handleLogout() {
        const result = await authService.logout();
        if (result.success) {
            clearUsername();
            navigate('/home');
        }
    }

    if (loading) {
        return (
            <div className="profile-container">
                <div className="profile-content">
                    <p>Loading profile...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="profile-container">
                <div className="profile-content">
                    <p className="auth-error">Error: {error}</p>
                    <button className="auth-submit" onClick={() => window.location.reload()}>Retry</button>
                </div>
            </div>
        );
    }

    return (
        <div className="profile-container">
            <div className="profile-content">
                <div className="profile-header">
                    <div className="profile-avatar">
                        {userInfo?.name.charAt(0).toUpperCase()}
                    </div>
                    <h1 className="profile-username">{userInfo?.name}</h1>
                    <p className="profile-email">{userInfo?.email}</p>
                    <div className="profile-balance">
                        💰 Balance: {userInfo?.balance}
                    </div>
                </div>

                <div className="profile-stats-grid">
                    <div className="stat-card">
                        <span className="stat-value">{stats?.gamesPlayed || 0}</span>
                        <span className="stat-label">Games Played</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-value">{stats?.wins || 0}</span>
                        <span className="stat-label">Wins</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-value">{stats?.losses || 0}</span>
                        <span className="stat-label">Losses</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-value">
                            {stats ? `${(stats.winRate * 100).toFixed(1)}%` : '0%'}
                        </span>
                        <span className="stat-label">Win Rate</span>
                    </div>
                </div>

                <div className="profile-actions">
                    <button onClick={handleLogout} className="profile-logout-btn">
                        Logout
                    </button>
                </div>
            </div>
        </div>
    );
}
