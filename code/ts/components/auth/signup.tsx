import React, {useReducer} from 'react'
import {Navigate, useLocation, Link} from 'react-router-dom'
import {isOk} from "../../services/utils";
import {authService} from "../../services/authService";
import {useAuthentication} from "../../providers/authentication";

type State =
    | {
    type: 'editing';
    inputs: { name: string; email: string; password: string; confirmPassword: string; invite: string };
    showPassword: boolean;
    error: string | null;
    shouldRedirect: boolean;
    passwordCriteria: {
        blank: boolean;
        maxLength: boolean;
    };
}
    | { type: 'redirect' }
    | {
    type: 'submitting';
    inputs: { name: string; email: string; password: string; confirmPassword: string; invite: string };
    showPassword: boolean;
    error: string | null;
    isLoading: boolean;
    shouldRedirect: boolean;
    passwordCriteria: {
        blank: boolean;
        maxLength: boolean;
    };
}

type Action =
    | { type: 'edit'; inputName: string; inputValue: string }
    | {
    type: 'submit';
    inputs: { name: string; email: string; password: string; confirmPassword: string; invite: string }
}
    | { type: 'togglePassword' }
    | { type: 'setError'; error: string | null }
    | { type: 'setRedirect' }
    | {
    type: 'updatePasswordCriteria'; criteria: {
        blank: boolean;
        maxLength: boolean;
    }
}

function logUnexpectedAction(state: State, action: Action) {
    // console.log(`Unexpected action '${action.type} on state '${state.type}'`)
}

function reduce(state: State, action: Action): State {
    switch (state.type) {
        case 'editing':
            switch (action.type) {
                case 'edit':
                    const newInputs = {...state.inputs, [action.inputName]: action.inputValue}
                    return {
                        ...state,
                        inputs: newInputs,
                    }
                case 'submit':
                    return {
                        type: 'submitting',
                        inputs: action.inputs,
                        showPassword: state.showPassword,
                        error: null,
                        isLoading: true,
                        shouldRedirect: false,
                        passwordCriteria: state.passwordCriteria
                    }
                case 'togglePassword':
                    return {...state, showPassword: !state.showPassword}
                case 'updatePasswordCriteria':
                    return {...state, passwordCriteria: action.criteria}
                default:
                    logUnexpectedAction(state, action)
                    return state
            }
        case 'submitting':
            switch (action.type) {
                case 'setError':
                    return {
                        type: 'editing',
                        inputs: {...state.inputs, password: '', confirmPassword: ''},
                        showPassword: false,
                        error: action.error,
                        shouldRedirect: false,
                        passwordCriteria: {
                            blank: false,
                            maxLength: false
                        }
                    }
                case 'setRedirect':
                    return {type: 'redirect'}
                default:
                    logUnexpectedAction(state, action)
                    return state
            }
        default:
            logUnexpectedAction(state, action)
            return state
    }
}

export function Signup() {
    const [state, dispatch] = useReducer(reduce, {
        type: 'editing',
        inputs: {name: '', email: '', password: '', confirmPassword: '', invite: ''},
        showPassword: false,
        error: null,
        shouldRedirect: false,
        passwordCriteria: {
            blank: false,
            maxLength: false,
        }
    })

    const [, setUsername] = useAuthentication();
    const location = useLocation()

    if (state.type === 'redirect') {
        return <Navigate to={location.state?.source || '/'} replace={true}/>
    }

    function validatePassword(password: string) {
        const criteria = {
            blank: password.length > 0,
            maxLength: password.length <= 30
        }
        dispatch({type: 'updatePasswordCriteria', criteria})
        return criteria
    }

    function handleChange(ev: React.ChangeEvent<HTMLInputElement>) {
        dispatch({type: 'edit', inputName: ev.target.name, inputValue: ev.target.value})

        if (ev.target.name === 'password') {
            validatePassword(ev.target.value)
        }
    }

    async function handleSubmit(ev: React.FormEvent<HTMLFormElement>) {
        ev.preventDefault()
        if (state.type === 'editing') {
            const {password, confirmPassword} = state.inputs
            if (password !== confirmPassword) {
                dispatch({type: 'setError', error: 'Passwords do not match'})
                return
            }
            const criteria = validatePassword(password)
            if (!Object.values(criteria).every(Boolean)) {
                dispatch({type: 'setError', error: 'Password does not meet all requirements'})
                return
            }
            dispatch({type: 'submit', inputs: state.inputs})
            const result = await authService.signup(state.inputs)
            if (isOk(result)) {
                const userInfoResult = await authService.getUserInfo()

                if (isOk(userInfoResult)) {
                    const userInfo = userInfoResult.value
                    localStorage.setItem('userId', userInfo.id.toString())
                    localStorage.setItem('username', userInfo.name)
                    localStorage.setItem('userEmail', userInfo.email)
                    setUsername(userInfo.name)
                    dispatch({type: 'setRedirect'})
                } else {
                    dispatch({type: 'setError', error: 'Failed to fetch user information. Please try again.'})
                }
            } else {
                dispatch({type: 'setError', error: result.error})
            }
        }
    }

    const inputs = state.type === 'editing' || state.type === 'submitting'
        ? state.inputs
        : {name: '', email: '', password: '', confirmPassword: '', invite: ''}

    return (
        <div className="auth-container">
            <h1 className="auth-title">Sign Up</h1>
            <form onSubmit={handleSubmit}>
                <fieldset disabled={state.type === 'submitting'}>
                    <div className="auth-form-group">
                        <div>
                            <label htmlFor="name" className="auth-label">
                                Username
                            </label>
                            <input
                                className="auth-input"
                                type="text"
                                id="name"
                                name="name"
                                value={inputs.name}
                                onChange={handleChange}
                                placeholder="Enter your username"
                                required
                            />
                        </div>
                        <div>
                            <label htmlFor="email" className="auth-label">
                                Email
                            </label>
                            <input
                                className="auth-input"
                                type="email"
                                id="email"
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
                                    type={state.showPassword ? "text" : "password"}
                                    id="password"
                                    name="password"
                                    value={inputs.password}
                                    onChange={handleChange}
                                    placeholder="Enter your password"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => dispatch({type: 'togglePassword'})}
                                    className="auth-toggle-password"
                                >
                                    {state.showPassword ? '♣️' : '♦️️'}
                                </button>
                            </div>
                            <ul className="auth-criteria-list">
                                {Object.entries(state.passwordCriteria).map(([key, value]) => (
                                    <li key={key}
                                        className={`auth-criteria-item ${value ? 'auth-criteria-success' : 'auth-criteria-error'}`}>
                                        {value ? '✓' : '×'} {
                                        key === 'maxLength' ? 'Less than 30 characters' :
                                            key === 'blank' ? 'Password must not be blank' :
                                                'Password is not blank'}
                                    </li>
                                ))}
                            </ul>
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="auth-label">
                                Confirm Password
                            </label>
                            <input
                                className="auth-input"
                                type="password"
                                id="confirmPassword"
                                name="confirmPassword"
                                value={inputs.confirmPassword}
                                onChange={handleChange}
                                placeholder="Confirm your password"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="invite" className="auth-label">
                                Invite Code
                            </label>
                            <input
                                className="auth-input"
                                type="text"
                                id="invite"
                                name="invite"
                                value={inputs.invite}
                                onChange={handleChange}
                                placeholder="Enter your invite code"
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={state.type === 'editing' && (
                                state.inputs.password !== state.inputs.confirmPassword ||
                                Object.values(state.passwordCriteria).some(criteria => !criteria)
                            )}
                            className="auth-submit"
                        >
                            Sign Up
                        </button>
                    </div>
                </fieldset>

                <div className="auth-links">
                    <p className="auth-text">
                        Already have an account?{' '}
                        <Link to="/" className="auth-link">
                            Login
                        </Link>
                    </p>
                </div>

                {state.error && (
                    <div className="auth-error">
                        {state.error}
                    </div>
                )}
            </form>
        </div>
    );
}