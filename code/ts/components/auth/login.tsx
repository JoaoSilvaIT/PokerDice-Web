import React, {useReducer} from 'react';
import {useAuthentication} from "../../providers/authentication";
import { Navigate, useLocation, Link } from 'react-router-dom';
import { authService} from "../../services/authService";
import {isOk} from "../../services/utils";
import '../../styles/login.css';

type State =
    | {type: 'editing',
    inputs: {email: string; password: string},
    error: string | null,
    redirect: boolean
}
    | {type: 'redirecting'}
    | {type: 'submitting',
    inputs: {email: string; password: string},
    error: string | null,
    isLoading: boolean,
    redirect: boolean
}

type Action =
    | {type: 'edit'; inputName: string; value: string}
    | {type: 'submit'; inputs: {email: string; password: string}}
    | {type: 'setError'; error: string | null}
    | {type: 'setRedirect';}
    | {type: 'setLoading'; isLoading: boolean}

function unexpectedAction(action: Action, state: State){
    console.log(`Unauthorized action: ${action.type} in state: ${state.type}`);
    return state
}

function reduce(state: State, action: Action): State {
    switch (state.type) {
        case 'editing':
            switch (action.type) {
                case 'edit':
                    return {...state, inputs: {...state.inputs, [action.inputName]: action.value}}
                case 'submit':
                    return {
                        type: 'submitting',
                        inputs: action.inputs,
                        error: null,
                        isLoading: true,
                        redirect: false
                    }
                default:
                    unexpectedAction(action,state)
                    return state
            }
        case 'submitting':
            switch (action.type) {
                case 'setError':
                    return{
                        type: 'editing',
                        inputs: {...state.inputs, password: ''},
                        error: action.error,
                        redirect: false
                    }
                case 'setRedirect':
                    return {type: 'redirecting'}
                default:
                    unexpectedAction(action,state)
                    return state
            }
        default:
            unexpectedAction(action,state)
            return state
    }
}

export function Login(){
    const [state, dispatch] = useReducer(reduce, {
        type: 'editing',
        inputs: {email: '', password: ''},
        error: null,
        redirect: false
    })
    const [,setUsername] = useAuthentication();
    const location = useLocation()

    if (state.type === 'redirecting') {
        return <Navigate to={location.state?.source || '/home'} replace={true} />
    }

    function handleChange(ev: React.ChangeEvent<HTMLInputElement>) {
        dispatch({ type: 'edit', inputName: ev.target.name, value: ev.target.value })
    }

    async function handleSubmit(ev: React.FormEvent<HTMLFormElement>) {
        ev.preventDefault()
        if (state.type === 'editing') {
            dispatch({ type: 'submit', inputs: state.inputs })
            const result = await authService.login(state.inputs)

            if (isOk(result)) {
                setUsername(state.inputs.email)
                dispatch({ type: 'setRedirect'})
            } else {
                dispatch({ type: 'setError', error: result.error })
            }
        }
    }

    const inputs = state.type === 'editing' || state.type === 'submitting'
        ? state.inputs
        : { email: '', password: '' }

    return (
        <div className="auth-container">
            <h1 className="auth-title">Login</h1>
            <form onSubmit={handleSubmit}>
                <fieldset disabled={state.type === 'submitting' && state.isLoading}>
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
                                value={inputs.email}
                                onChange={handleChange}
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
                                    value={inputs.password}
                                    onChange={handleChange}
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

                {state.type === 'editing' && state.error && (
                    <div className="auth-error">{state.error}</div>
                )}

                {state.type === 'submitting' && (
                    <div className="auth-loading">Loading...</div>
                )}
            </form>
        </div>
    );
}