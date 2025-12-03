package pt.isel.model.user

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.UserAuthService
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.utils.Either

@RestController
class UserController(
    private val userService: UserAuthService,
) {
    @PostMapping("/api/users")
    fun createUser(
        @RequestBody userInput: UserInput,
    ): ResponseEntity<*> {
        return when (val result = userService.createUser(userInput.name, userInput.email, userInput.password, userInput.invite)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        "/api/users/${result.value.id}",
                    ).body(UserOutputModel.fromDomain(result.value))

            is Either.Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping("/api/users/token")
    fun token(
        @RequestBody input: UserCreateTokenInputModel,
    ): ResponseEntity<*> {
        return when (val result = userService.createToken(input.email, input.password)) {
            is Either.Success -> {
                val token = result.value.tokenValue
                val cookieHeader = "token=$token; Path=/; Max-Age=${24 * 60 * 60}; SameSite=Strict"

                ResponseEntity
                    .status(HttpStatus.OK)
                    .header("Set-Cookie", cookieHeader)
                    .body(UserCreateTokenOutputModel(result.value.tokenValue))
            }

            is Either.Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping("/api/users/invite")
    fun createInvite(user: AuthenticatedUser): ResponseEntity<*> {
        return when (val result = userService.createAppInvite(user.user.id)) {
            is Either.Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(InviteOutputModel(result.value))

            is Either.Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping("/api/users/logout")
    fun logout(user: AuthenticatedUser): ResponseEntity<Unit> {
        userService.revokeToken(user.token)

        val cookieHeader = "token=; Path=/; Max-Age=0"

        return ResponseEntity
            .noContent()
            .header("Set-Cookie", cookieHeader)
            .build()
    }

    @GetMapping("/api/me")
    fun userHome(user: AuthenticatedUser): ResponseEntity<UserHomeOutputModel> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserHomeOutputModel(
                    id = user.user.id,
                    name = user.user.name,
                    email = user.user.email,
                ),
            )
}
