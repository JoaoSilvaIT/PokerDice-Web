package pt.isel.model.user

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.UserAuthService
import pt.isel.domain.AuthenticatedUser
import pt.isel.errors.AuthTokenError
import pt.isel.utils.Either

@RestController
class UserController(
    private val userService: UserAuthService,
) {
    @PostMapping("/api/users")
    fun createUser(
        @RequestBody userInput: UserInput,
    ): ResponseEntity<*> {
        val user =
            userService
                .createUser(userInput.name, userInput.email, userInput.password)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header(
                "Location",
                "/api/users/${user.id}",
            ).build<Unit>()
    }

    @PostMapping("/api/users/token")
    fun token(
        @RequestBody input: UserCreateTokenInputModel,
    ): ResponseEntity<*> {
        return when (val result = userService.createToken(input.email, input.password)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(UserCreateTokenOutputModel(result.value.tokenValue))

            is Either.Failure -> {
                val status =
                    when (result.value) {
                        is AuthTokenError.BlankEmail, is AuthTokenError.BlankPassword -> HttpStatus.BAD_REQUEST
                        is AuthTokenError.UserNotFoundOrInvalidCredentials -> HttpStatus.UNAUTHORIZED
                    }
                ResponseEntity
                    .status(status)
                    .body(mapOf("error" to (result.value::class.simpleName ?: "AuthError")))
            }
        }
    }

    @PostMapping("api/logout")
    fun logout(user: AuthenticatedUser) {
        userService.revokeToken(user.token)
    }

    @GetMapping("/api/me")
    fun userHome(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<UserHomeOutputModel> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserHomeOutputModel(
                    id = userAuthenticatedUser.user.id,
                    name = userAuthenticatedUser.user.name,
                    email = userAuthenticatedUser.user.email,
                ),
            )
}
