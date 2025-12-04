import React from 'react'
import { useAuthentication } from '../../providers/authentication'
import { Navigate} from 'react-router-dom'
import { useLocation } from 'react-router-dom'

export function RequireAuthentication({ children }) {
    const [username] = useAuthentication()
    const hasCookie = document.cookie.includes('token')
    const location = useLocation()

    if (username && hasCookie) {
        return children
    }

    return <Navigate to="/login" state={{ source: location.pathname }} replace={true} />
}