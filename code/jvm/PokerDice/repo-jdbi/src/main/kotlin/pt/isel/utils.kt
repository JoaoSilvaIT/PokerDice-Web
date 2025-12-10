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

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())
    registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
    registerColumnMapper(TokenValidationInfo::class.java, TokenValidationInfoMapper())
    registerColumnMapper(Instant::class.java, InstantMapper())
    return this
}