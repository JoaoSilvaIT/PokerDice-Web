package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import pt.isel.domain.games.Game
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.User
import pt.isel.errors.GameError
import pt.isel.mem.TransactionManagerInMem
import pt.isel.utils.Either
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@SpringJUnitConfig(TestConfig::class)
class GameServiceTest {
    @Autowired
    private lateinit var serviceGame: GameService

    @Autowired
    private lateinit var serviceLobby: LobbyService

    @Autowired
    private lateinit var serviceUser: UserAuthService

    @Autowired
    private lateinit var trxManager: TransactionManagerInMem

    @Autowired
    private lateinit var inviteDomain: pt.isel.domain.users.InviteDomain

    @Autowired
    private lateinit var clock: java.time.Clock

    @BeforeEach
    fun setup() {
        trxManager.run {
            repoUsers.clear()
            repoLobby.clear()
            repoGame.clear()
            (repoInvite as pt.isel.mem.RepositoryInviteInMem).clear()
        }
    }

    private fun createValidInvite(): String {
        val inviteCode = inviteDomain.generateInviteValue()
        trxManager.run {
            repoInvite.createAppInvite(
                inviterId = 1,
                inviteValidationInfo = inviteDomain.createInviteValidationInformation(inviteCode),
                state = inviteDomain.validState,
                createdAt = clock.instant(),
            )
        }
        return inviteCode
    }

    @Test
    fun `createGame should create and return a game`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val lobby = lobbyResult.value

        val result = serviceGame.createGame(System.currentTimeMillis(), lobby.id, 5, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(5, result.value.numberOfRounds)
        assertEquals(lobby.id, result.value.lobbyId)
    }

    @Test
    fun `createGame fails with invalid number of rounds`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 0, host.id)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidNumberOfRounds, result.value)
    }

    @Test
    fun `createGame fails with invalid time`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(-1, lobbyResult.value.id, 5, host.id)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidTime, result.value)
    }

    @Test
    fun `createGame by lobbyId should create and return a game`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 5, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(5, result.value.numberOfRounds)
    }

    @Test
    fun `createGame by lobbyId fails when lobby not found`() {
        val result = serviceGame.createGame(System.currentTimeMillis(), 999, 5, 1)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.LobbyNotFound, result.value)
    }

    @Test
    fun `getGame should return game when it exists`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val gameResult = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val game = serviceGame.getGame(gameResult.value.id)

        assertNotNull(game)
        assertEquals(gameResult.value.id, game.id)
    }

    @Test
    fun `getGame should return null when game does not exist`() {
        val game = serviceGame.getGame(999)

        kotlin.test.assertNull(game)
    }

    @Test
    fun `endGame should end a game successfully`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)
        val game = gameResult.value

        val result = serviceGame.endGame(game.id, startTime + 1000)

        assertIs<Either.Success<Game>>(result)
        assertNotNull(result.value.endedAt)
    }

    @Test
    fun `endGame fails with invalid end time before start time`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.id, startTime - 1000)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidTime, result.value)
    }

    @Test
    fun `endGame fails when game already ended`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)
        serviceGame.endGame(gameResult.value.id, startTime + 1000)

        val result = serviceGame.endGame(gameResult.value.id, startTime + 2000)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.GameAlreadyEnded, result.value)
    }

    @Test
    fun `endGame by gameId should end a game successfully`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.id, startTime + 1000)

        assertIs<Either.Success<Game>>(result)
        assertNotNull(result.value.endedAt)
    }

    @Test
    fun `endGame by gameId fails when game not found`() {
        val result = serviceGame.endGame(999, System.currentTimeMillis())

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.GameNotFound, result.value)
    }

    @Test
    fun `getGame should return all game details correctly`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val game = serviceGame.getGame(gameResult.value.id)

        assertNotNull(game)
        assertEquals(gameResult.value.id, game.id)
        assertEquals(5, game.numberOfRounds)
        assertEquals(lobbyResult.value.id, game.lobbyId)
        kotlin.test.assertNull(game.endedAt)
    }

    @Test
    fun `createGame should set correct timestamps`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()

        val result = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(startTime, result.value.startedAt)
        kotlin.test.assertNull(result.value.endedAt)
    }

    @Test
    fun `endGame should set correct end timestamp`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)
        val endTime = startTime + 5000

        val result = serviceGame.endGame(gameResult.value.id, endTime)

        assertIs<Either.Success<Game>>(result)
        assertEquals(endTime, result.value.endedAt)
    }

    @Test
    fun `createGame should accept maximum valid number of rounds`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 100, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(100, result.value.numberOfRounds)
    }

    @Test
    fun `createGame should accept minimum valid number of rounds`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 1, host.id)

        assertIs<Either.Success<Game>>(result)
        assertEquals(1, result.value.numberOfRounds)
    }

    @Test
    fun `createGame fails with negative number of rounds`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)

        val result = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, -5, host.id)

        assertIs<Either.Failure<GameError>>(result)
        assertEquals(GameError.InvalidNumberOfRounds, result.value)
    }

    @Test
    fun `endGame with same timestamp as start time should succeed`() {
        val invite = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val startTime = System.currentTimeMillis()
        val gameResult = serviceGame.createGame(startTime, lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)

        val result = serviceGame.endGame(gameResult.value.id, startTime)

        assertIs<Either.Success<Game>>(result)
        assertEquals(startTime, result.value.endedAt)
    }

    @Test
    fun `nextTurn should end game if only one player can afford ante`() {
        // Setup users
        val invite1 = createValidInvite()
        val hostResult = serviceUser.createUser("Host", "host@example.com", "password123", invite1)
        assertIs<Either.Success<User>>(hostResult)
        val host = hostResult.value

        val invite2 = createValidInvite()
        val playerResult = serviceUser.createUser("Player", "player@example.com", "password123", invite2)
        assertIs<Either.Success<User>>(playerResult)
        val player = playerResult.value

        // Setup Lobby & Game
        val lobbyResult = serviceLobby.createLobby(host, "Poker Room", "desc", 2, 2)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        serviceLobby.joinLobby(lobbyResult.value.id, player)

        val gameResult = serviceGame.createGame(System.currentTimeMillis(), lobbyResult.value.id, 5, host.id)
        assertIs<Either.Success<Game>>(gameResult)
        var game = gameResult.value

        // Start Game
        val startedGameResult = serviceGame.startGame(game.id, host.id)
        assertIs<Either.Success<Game>>(startedGameResult)
        game = startedGameResult.value

        // Start Round 1
        val round1Result = serviceGame.startNewRound(game.id, null)
        assertIs<Either.Success<Game>>(round1Result)
        game = round1Result.value
        val round1 = game.currentRound!!

        // Set Ante (e.g. 10)
        val ante = 10
        val setAnteResult = serviceGame.setAnte(game.id, ante, round1.turn.player.id)
        assertIs<Either.Success<Game>>(setAnteResult)
        game = setAnteResult.value

        // Pay Ante
        val payAnteResult = serviceGame.payAnte(game.id)
        assertIs<Either.Success<Game>>(payAnteResult)
        game = payAnteResult.value

        // Set one player's balance to 0 (Bankruptcy simulation)
        // We need to update the Game object in repo directly because InMem Game repo doesn't sync with User repo automatically
        trxManager.run {
            val g = repoGame.findById(game.id)!!
            val updatedPlayers =
                g.players.map {
                    if (it.id == player.id) it.copy(currentBalance = 0) else it
                }
            val updatedGame = g.copy(players = updatedPlayers)
            repoGame.save(updatedGame)
        }

        // Complete the round so nextTurn is called
        // We need to roll dice for all players
        // This is tedious to simulate full round.
        // Instead, we can call 'startNewRound' directly which triggers the check?
        // But 'startNewRound' checks eligibility.
        // If we call 'nextTurn' on the last turn, it triggers 'startNewRound'.

        // Let's try calling startNewRound directly for Round 2.
        // But startNewRound checks if previous round is finished.
        // "if (round.winners.isEmpty() && round.number != 0) return failure(GameError.RoundWinnerNotDecided)"

        // So we must finish round 1.
        // Shortcut: Update the game state in repo to make round 1 finished?
        // Or just force start a new round if we can bypass the check? No.

        // Let's manually set a winner for Round 1 in the repo?
        trxManager.run {
            val g = repoGame.findById(game.id)!!
            val r = g.currentRound!!
            val winners = listOf(g.players.first { it.id == host.id })
            val finishedRound = r.copy(winners = winners)
            repoGame.updateGameRound(finishedRound, g)
        }

        // Now try to start Round 2
        val round2Result = serviceGame.startNewRound(game.id, null)

        // Expectation: Since 'player' has 0 balance (less than MIN_ANTE which is likely > 0),
        // they should be excluded.
        // Remaining players: 1 (host).
        // Since < 2 players, game should END.

        assertIs<Either.Success<Game>>(round2Result)
        val gameRound2 = round2Result.value

        assertEquals(pt.isel.domain.games.utils.State.FINISHED, gameRound2.state)
        assertNotNull(gameRound2.endedAt)
    }

    private fun createUser(name: String): User {
        val invite = createValidInvite()
        val result = serviceUser.createUser(name, "$name@example.com", "password", invite)
        return (result as Either.Success).value
    }

    @Test
    fun `3 players, 1 bankrupt - Game should continue with 2 players`() {
        // 1. Setup 3 Users
        val p1 = createUser("P1")
        val p2 = createUser("P2")
        val p3 = createUser("P3")

        // 2. Create Lobby & Join
        val lobbyResult = serviceLobby.createLobby(p1, "Room", "Desc", 2, 4)
        assertIs<Either.Success<Lobby>>(lobbyResult)
        val lobbyId = lobbyResult.value.id
        serviceLobby.joinLobby(lobbyId, p2)
        serviceLobby.joinLobby(lobbyId, p3)

        // 3. Start Game
        val gameResult = serviceGame.createGame(System.currentTimeMillis(), lobbyId, 5, p1.id)
        assertIs<Either.Success<Game>>(gameResult)
        var game = gameResult.value
        serviceGame.startGame(game.id, p1.id)

        // 4. Start Round 1
        val r1Result = serviceGame.startNewRound(game.id, null)
        assertIs<Either.Success<Game>>(r1Result)
        game = r1Result.value

        // 5. Simulate Gameplay - Set Ante & Pay
        val round1 = game.currentRound!!
        serviceGame.setAnte(game.id, 10, round1.turn.player.id)
        serviceGame.payAnte(game.id)

        // 6. Force P3 Bankruptcy
        trxManager.run {
            val g = repoGame.findById(game.id)!!
            val updatedPlayers =
                g.players.map {
                    if (it.id == p3.id) it.copy(currentBalance = 0) else it
                }
            val updatedGame = g.copy(players = updatedPlayers)
            repoGame.save(updatedGame)

            // Hack to finish round 1 in repo
            val r = g.currentRound!!
            val winners = listOf(g.players.first { it.id == p1.id }) // P1 wins
            val finishedRound = r.copy(winners = winners)
            repoGame.updateGameRound(finishedRound, updatedGame)
        }

        // 7. Attempt to start Round 2
        // Since P3 is bankrupt, Round 2 should start with only P1 and P2
        val r2Result = serviceGame.startNewRound(game.id, null)
        assertIs<Either.Success<Game>>(r2Result)
        game = r2Result.value

        // Assertions
        assertEquals(pt.isel.domain.games.utils.State.RUNNING, game.state, "Game should still be RUNNING")
        val round2 = game.currentRound!!
        assertEquals(2, round2.number)
        assertEquals(2, round2.players.size, "Round 2 should have 2 players")

        val pIds = round2.players.map { it.id }
        kotlin.test.assertTrue(pIds.contains(p1.id))
        kotlin.test.assertTrue(pIds.contains(p2.id))
        kotlin.test.assertTrue(!pIds.contains(p3.id), "P3 should be excluded")
    }

    @Test
    fun `3 players, 2 bankrupt - Game should END`() {
        // 1. Setup 3 Users
        val p1 = createUser("P1")
        val p2 = createUser("P2")
        val p3 = createUser("P3")

        // 2. Create Lobby & Join
        val lobbyResult = serviceLobby.createLobby(p1, "Room", "Desc", 2, 4)
        val lobbyId = (lobbyResult as Either.Success).value.id
        serviceLobby.joinLobby(lobbyId, p2)
        serviceLobby.joinLobby(lobbyId, p3)

        // 3. Start Game
        val gameResult = serviceGame.createGame(System.currentTimeMillis(), lobbyId, 5, p1.id)
        var game = (gameResult as Either.Success).value
        serviceGame.startGame(game.id, p1.id)

        // 4. Start Round 1
        serviceGame.startNewRound(game.id, null)

        // 5. Force P2 & P3 Bankruptcy
        trxManager.run {
            val g = repoGame.findById(game.id)!!
            val updatedPlayers =
                g.players.map {
                    if (it.id == p2.id || it.id == p3.id) it.copy(currentBalance = 0) else it
                }
            val updatedGame = g.copy(players = updatedPlayers)
            repoGame.save(updatedGame)

            // Hack to finish round 1
            val r = g.currentRound!!
            val winners = listOf(g.players.first { it.id == p1.id })
            val finishedRound = r.copy(winners = winners)
            repoGame.updateGameRound(finishedRound, updatedGame)
        }

        // 6. Attempt to start Round 2
        val r2Result = serviceGame.startNewRound(game.id, null)

        // Assertions
        // Game should transition to FINISHED because only 1 player (P1) is left
        assertIs<Either.Success<Game>>(r2Result)
        game = r2Result.value

        assertEquals(pt.isel.domain.games.utils.State.FINISHED, game.state, "Game should be FINISHED")
        assertNotNull(game.endedAt)
    }
}
