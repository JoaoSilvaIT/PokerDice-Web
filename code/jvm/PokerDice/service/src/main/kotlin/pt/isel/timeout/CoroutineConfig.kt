package pt.isel.timeout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {
    @Bean
    fun lobbyScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
