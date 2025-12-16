import React, {useContext, createContext, useState, ReactNode} from 'react'

interface AuthContextType {
    username: string | undefined;
    setUsername: (username: string) => void;
    clearUsername: () => void;
}

const AuthenticationContext = createContext<AuthContextType>({
    username: undefined,
    setUsername: () => {},
    clearUsername: () => {}
})

interface AuthProviderProps {
    children: ReactNode;
}

export function AuthenticationProvider({children}: AuthProviderProps) {
    const [username, setUsernameState] = useState<string | undefined>(() =>
        localStorage.getItem('username') || undefined
    )

    const clearUsername = () => {
        setUsernameState(undefined)
        localStorage.removeItem('username')
        localStorage.removeItem('userId')
        localStorage.removeItem('userEmail')
    }

    const setUsername = (newUsername: string) => {
        localStorage.setItem('username', newUsername)
        setUsernameState(newUsername)
    }

    const value = { username, setUsername, clearUsername }
    return <AuthenticationContext.Provider value={value}>{children}</AuthenticationContext.Provider>
}

export function useAuthentication(): [string | undefined, (username: string) => void, () => void] {
    const context = useContext(AuthenticationContext)
    return [
        context.username,
        context.setUsername,
        context.clearUsername
    ]
}
