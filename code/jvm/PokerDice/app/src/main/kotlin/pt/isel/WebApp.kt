package pt.isel

import pt.isel.domain.users.Sha256InviteEncoder
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.domain.users.InviteDomainConfig
import pt.isel.domain.users.Sha256TokenEncoder
import pt.isel.domain.users.UsersDomainConfig
import pt.isel.repo.JdbiTransactionManager
import java.time.Clock
import java.time.Duration
import kotlin.time.Duration.Companion.hours

@Configuration
class PipelineConfigurer(
    val authenticationInterceptor: AuthenticationInterceptor,
    val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}

@SpringBootApplication(scanBasePackages = ["pt.isel"])
class WebApp {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun inviteEncoder() = Sha256InviteEncoder()

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
            minPasswordLength = 2,
        )

    @Bean
    fun jdbi(): Jdbi =
        Jdbi.create(
            PGSimpleDataSource().apply {
                setURL(
                    Environment.getDbUrl(),
                )
            },
        ).configureWithAppRequirements()

    @Bean
    fun inviteDomainConfig() =
        InviteDomainConfig(
            validState = "pending",
            expireInviteTime = 24.hours,
            expiredState = "expired",
            usedState = "used",
            declinedState = "declined",
        )
}

fun main() {
    runApplication<WebApp>()
}

// DB_URL = jdbc:postgresql://localhost:5432/pokerdice?user=pokerdice&password=password
