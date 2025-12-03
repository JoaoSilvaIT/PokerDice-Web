package pt.isel

import jakarta.servlet.http.Cookie
import org.springframework.stereotype.Component
import pt.isel.domain.users.AuthenticatedUser

@Component
class RequestTokenProcessor(
    private val usersService: UserAuthService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return usersService.getUserByToken(parts[1])?.let {
            AuthenticatedUser(
                it,
                parts[1],
            )
        }
    }

    fun processCookieToken(cookies: Array<Cookie>?): AuthenticatedUser? {
        val tokenCookie = cookies?.find { it.name == "token" }
        tokenCookie?.value?.let { token ->
            return usersService.getUserByToken(token)?.let {
                AuthenticatedUser(
                    it,
                    token,
                )
            }
        }
        return null
    }

    companion object {
        const val SCHEME = "bearer"
    }
}
