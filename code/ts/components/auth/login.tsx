import React, {useState} from 'react';
import {useAuthentication} from "../../providers/authentication";
import {useNavigate, useLocation, Link} from 'react-router-dom';
import {authService} from "../../services/authService";
import {isOk, formatError} from "../../services/utils";
import {ToastContainer, useToast} from '../generic/toast';
import '../../styles/login.css';

export function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const [, setUsername] = useAuthentication();
    const location = useLocation();
    const {toasts, removeToast, showError} = useToast();


    async function handleSubmit(ev: React.FormEvent<HTMLFormElement>) {
        ev.preventDefault();
        setLoading(true);

        const result = await authService.login({email, password});

        if (isOk(result)) {
            const userInfoResult = await authService.getUserInfo();

            if (isOk(userInfoResult)) {
                const userInfo = userInfoResult.value;
                localStorage.setItem('userId', userInfo.id.toString());
                localStorage.setItem('username', userInfo.name);
                localStorage.setItem('userEmail', userInfo.email);
                setUsername(userInfo.name);
                navigate('/home', { replace: true });
            } else {
                showError('Failed to fetch user information. Please try again.');
                setPassword('');
            }
        } else {
            let msg = formatError(result.error);
            if (msg.toLowerCase().includes('user not found or invalid credentials')) {
                msg = 'Invalid email or password';
            }
            showError(msg);
            setPassword('');
        }
        setLoading(false);
    }

    return (
        <div className="auth-container">
            <ToastContainer toasts={toasts} removeToast={removeToast} />
            <h1 className="auth-title">Login</h1>
            <form onSubmit={handleSubmit}>
                <fieldset disabled={loading}>
                    <div className="auth-form-group">
                        <div>
                            <label htmlFor="email" className="auth-label">
                                Email
                            </label>
                            <input
                                className="auth-input"
                                id="email"
                                type="email"
                                name="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="Enter your email"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="password" className="auth-label">
                                Password
                            </label>
                            <div className="auth-password-container">
                                <input
                                    className="auth-input"
                                    id="password"
                                    type="password"
                                    name="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Enter your password"
                                    required
                                />
                            </div>
                        </div>

                        <button type="submit" className="auth-submit">
                            Sign In
                        </button>
                    </div>
                </fieldset>

                <div className="auth-links">
                    <p className="auth-text">
                        Don't have an account?{' '}
                        <Link to="/signup" className="auth-link">
                            Sign Up
                        </Link>
                    </p>
                </div>

                {loading && (
                    <div className="auth-loading">Loading...</div>
                )}
            </form>
        </div>
    );
}