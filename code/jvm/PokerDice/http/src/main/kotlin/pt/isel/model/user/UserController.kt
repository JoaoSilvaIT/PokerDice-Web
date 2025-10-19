package pt.isel.model.user

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.UserAuthService
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.errors.AuthTokenError
import pt.isel.model.Problem
import pt.isel.utils.Either

@RestController
class UserController(
    private val userService: UserAuthService,
) {
    @PostMapping("/api/users")
    fun createUser(
        @RequestBody userInput: UserInput,
    ): ResponseEntity<*> {
        return when (val result = userService.createUser(userInput.name, userInput.email, userInput.password)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        "/api/users/${result.value.id}",
                    ).body(UserOutputModel.fromDomain(result.value))

            is Either.Failure -> {
                when (result.value) {
                    is AuthTokenError.BlankName -> Problem.BlankName.response(HttpStatus.BAD_REQUEST)
                    is AuthTokenError.BlankEmail -> Problem.BlankEmail.response(HttpStatus.BAD_REQUEST)
                    is AuthTokenError.BlankPassword -> Problem.BlankPassword.response(HttpStatus.BAD_REQUEST)
                    is AuthTokenError.EmailAlreadyInUse -> Problem.EmailAlreadyInUse.response(HttpStatus.CONFLICT)
                    is AuthTokenError.UserNotFoundOrInvalidCredentials ->
                        Problem.UserNotFoundOrInvalidCredentials.response(HttpStatus.UNAUTHORIZED)
                }
            }
        }
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
                when (result.value) {
                    is AuthTokenError.BlankEmail -> Problem.BlankEmail.response(HttpStatus.BAD_REQUEST)
                    is AuthTokenError.BlankPassword -> Problem.BlankPassword.response(HttpStatus.BAD_REQUEST)
                    is AuthTokenError.UserNotFoundOrInvalidCredentials ->
                        Problem.UserNotFoundOrInvalidCredentials.response(HttpStatus.UNAUTHORIZED)
                    is AuthTokenError.BlankName,
                    is AuthTokenError.EmailAlreadyInUse ->
                        Problem.BlankEmail.response(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @PostMapping("/api/logout")
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
