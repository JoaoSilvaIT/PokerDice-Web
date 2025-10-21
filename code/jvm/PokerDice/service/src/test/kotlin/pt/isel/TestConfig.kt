package pt.isel

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.domain.users.InviteDomainConfig
import pt.isel.domain.users.Sha256InviteEncoder
import pt.isel.domain.users.Sha256TokenEncoder
import pt.isel.domain.users.UsersDomainConfig
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repo.TransactionManager
import java.time.Clock
import java.time.Duration

@Configuration
@ComponentScan(
    basePackages = ["pt.isel"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ["pt\\.isel\\.repo\\.TransactionManagerJdbi"],
        ),
    ],
)
class TestConfig {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun inviteEncoder() = Sha256InviteEncoder()

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun trxManager(): TransactionManager = TransactionManagerInMem()

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
    fun inviteDomainConfig() =
        InviteDomainConfig(
            expireInviteTime = kotlin.time.Duration.parse("24h"),
            validState = "valid",
            expiredState = "expired",
            usedState = "used",
            declinedState = "declined",
        )
}
