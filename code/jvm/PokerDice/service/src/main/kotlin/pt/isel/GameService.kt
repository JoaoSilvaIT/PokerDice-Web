package pt.isel

import org.springframework.stereotype.Component

@Component
class GameService(
    private val repoGame: RepositoryGame,
)
