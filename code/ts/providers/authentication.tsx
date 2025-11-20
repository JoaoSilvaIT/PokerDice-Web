import React, {useContext} from 'react'
import {createContext, useState} from 'react'

const AuthenticationContext = createContext({
    username: undefined,
    setUsername: (_: string) => {
    },
    clearUsername: () => {}
})

export function AuthenticationProvider({children}) {
    const [username, setUsername] = useState<string | undefined>(() =>
        localStorage.getItem('username') || undefined
    )

    const clearUsername = () => {
        setUsername(undefined)
        localStorage.removeItem('username')
    }

    const value = {
        username: username,
        setUsername: (newUsername: string) => {
            localStorage.setItem('username', newUsername)
            setUsername(newUsername)
        },
        clearUsername: clearUsername
    }
    return <AuthenticationContext.Provider value={value}>{children}</AuthenticationContext.Provider>
}

export function useAuthentication() {
    const state = useContext(AuthenticationContext)

    return [
        state.username,
        (username: string) => {
            state.setUsername(username)
        },
        state.clearUsername
    ]
}
