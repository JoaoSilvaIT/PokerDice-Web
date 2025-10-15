package pt.isel

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper
import org.jdbi.v3.postgres.PostgresPlugin
import pt.isel.domain.users.PasswordValidationInfo
import pt.isel.domain.users.TokenValidationInfo
import pt.isel.domain.users.User
import pt.isel.mapper.InstantMapper
import pt.isel.mapper.PasswordValidationInfoMapper
import pt.isel.mapper.TokenValidationInfoMapper
import java.time.Instant

/**
 * Configures JDBI with all required plugins, mappers, and configurations
 * for the PokerDice application.
 */
fun Jdbi.configureWithAppRequirements(): Jdbi {
    // Install plugins
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())

    // Register custom column mappers for value objects
    registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
    registerColumnMapper(TokenValidationInfo::class.java, TokenValidationInfoMapper())
    registerColumnMapper(Instant::class.java, InstantMapper())

    // Register row mappers for domain entities
    registerRowMapper(ConstructorMapper.factory(User::class.java))

    return this
}
