package pt.isel.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import pt.isel.*
import pt.isel.domain.games.Dice
import pt.isel.domain.games.utils.Face
import pt.isel.domain.lobby.Lobby
import pt.isel.domain.users.User
import pt.isel.errors.GameError
import pt.isel.mem.RepositoryInviteInMem
import pt.isel.mem.TransactionManagerInMem
import pt.isel.utils.Either
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [TestConfig::class])
class GameWinnerIntegrationTest {

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
    private lateinit var clock: Clock

    private lateinit var hostUser: User
    private lateinit var player1User: User
    private lateinit var lobby: Lobby

    @BeforeEach
    fun setup() {
        // Clear all data
        trxManager.run {
            repoUsers.clear()
            repoLobby.clear()
            repoGame.clear()
            (repoInvite as RepositoryInviteInMem).clear()
        }

        // Create invites
        val invite1 = inviteDomain.generateInviteValue()
        val invite2 = inviteDomain.generateInviteValue()

        trxManager.run {
            repoInvite.createAppInvite(
                inviterId = 1,
                inviteValidationInfo = invite1,
                createdAt = clock.millis()
            )
            repoInvite.createAppInvite(
                inviterId = 1,
                inviteValidationInfo = invite2,
                createdAt = clock.millis()
            )
        }

        // Create users
        val hostResult = serviceUser.createUser("host", "host@test.com", "password123", invite1)
        val player1Result = serviceUser.createUser("player1", "player1@test.com", "password123", invite2)

        assertIs<Either.Right<User>>(hostResult)
        assertIs<Either.Right<User>>(player1Result)

        hostUser = hostResult.value
        player1User = player1Result.value

        // Create lobby
        val lobbyResult = serviceLobby.createLobby(
            "Test Lobby",
            "Integration test lobby",
            6,
            2,
            3,
            hostUser.id
        )

        assertIs<Either.Right<Lobby>>(lobbyResult)
        lobby = lobbyResult.value

        // Player1 joins lobby
        val joinResult = serviceLobby.joinLobby(lobby.id, player1User.id)
        assertIs<Either.Right<Lobby>>(joinResult)
    }

    @Test
    fun `test complete game flow with winner determination`() {
        println("\n=== Starting Game Winner Integration Test ===\n")

        // Step 1: Create game
        println("1. Creating game with 2 rounds...")
        val createGameResult = serviceGame.createGame(
            startedAt = clock.millis(),
            lobbyId = lobby.id,
            numberOfRounds = 2,
            creatorId = hostUser.id
        )
        assertIs<Either.Right<*>>(createGameResult, "Failed to create game")
        val game = createGameResult.value
        println("   Game created with ID: ${game.id}")

        // Step 2: Start game
        println("\n2. Starting game...")
        val startGameResult = serviceGame.startGame(game.id, hostUser.id)
        assertIs<Either.Right<*>>(startGameResult, "Failed to start game")
        println("   Game started")

        // === ROUND 1 ===
        println("\n=== ROUND 1 ===")

        // Step 3: Start round 1
        println("\n3. Starting round 1...")
        val startRound1Result = serviceGame.startNewRound(game.id)
        assertIs<Either.Right<*>>(startRound1Result, "Failed to start round 1")
        println("   Round 1 started")

        // Step 4: Set ante for round 1
        println("\n4. Setting ante to 25...")
        val setAnte1Result = serviceGame.setAnte(game.id, 25, hostUser.id)
        assertIs<Either.Right<*>>(setAnte1Result, "Failed to set ante")
        println("   Ante set to 25")

        // Step 5: Pay ante
        println("\n5. Paying ante...")
        val payAnte1Result = serviceGame.payAnte(game.id)
        assertIs<Either.Right<*>>(payAnte1Result, "Failed to pay ante")
        val gameAfterAnte1 = payAnte1Result.value
        assertEquals(50, gameAfterAnte1.currentRound?.pot, "Pot should be 50 after paying ante")
        println("   Ante paid. Pot = ${gameAfterAnte1.currentRound?.pot}")

        // Step 6-10: Host takes turn (roll and lock 5 dice)
        println("\n6-10. Host taking turn...")
        playTurn(game.id, hostUser.id, Face.ACE)

        // Step 11-15: Player1 takes turn (roll and lock 5 dice)
        println("\n11-15. Player1 taking turn...")
        playTurn(game.id, player1User.id, Face.NINE)

        // Step 16: Decide round 1 winner (ACE beats NINE, so host wins)
        println("\n16. Deciding round 1 winner...")
        val decideWinner1Result = serviceGame.decideRoundWinner(game.id)
        assertIs<Either.Right<*>>(decideWinner1Result, "Failed to decide round 1 winner")
        val round1Winners = decideWinner1Result.value
        assertEquals(1, round1Winners.size, "Should have exactly 1 winner")
        assertEquals(hostUser.name, round1Winners[0].name, "Host should be the winner")
        println("   Round 1 winner: ${round1Winners[0].name}")

        // Step 17: Distribute round 1 winnings
        println("\n17. Distributing round 1 winnings (pot = 50)...")
        val distributeWinnings1Result = serviceGame.distributeWinnings(game.id)
        assertIs<Either.Right<*>>(distributeWinnings1Result, "Failed to distribute round 1 winnings")
        println("   Winnings distributed")

        // === ROUND 2 ===
        println("\n=== ROUND 2 ===")

        // Step 18: Start round 2
        println("\n18. Starting round 2...")
        val startRound2Result = serviceGame.startNewRound(game.id)
        assertIs<Either.Right<*>>(startRound2Result, "Failed to start round 2")
        println("   Round 2 started")

        // Step 19: Set ante for round 2
        println("\n19. Setting ante to 50...")
        val setAnte2Result = serviceGame.setAnte(game.id, 50, player1User.id)
        assertIs<Either.Right<*>>(setAnte2Result, "Failed to set ante")
        println("   Ante set to 50")

        // Step 20: Pay ante
        println("\n20. Paying ante...")
        val payAnte2Result = serviceGame.payAnte(game.id)
        assertIs<Either.Right<*>>(payAnte2Result, "Failed to pay ante")
        val gameAfterAnte2 = payAnte2Result.value
        assertEquals(100, gameAfterAnte2.currentRound?.pot, "Pot should be 100 after paying ante")
        println("   Ante paid. Pot = ${gameAfterAnte2.currentRound?.pot}")

        // Step 21-25: Player1 takes turn (roll and lock 5 dice)
        println("\n21-25. Player1 taking turn...")
        playTurn(game.id, player1User.id, Face.KING)

        // Step 26-30: Host takes turn (roll and lock 5 dice)
        println("\n26-30. Host taking turn...")
        playTurn(game.id, hostUser.id, Face.NINE)

        // Step 31: Decide round 2 winner (KING beats NINE, so player1 wins)
        println("\n31. Deciding round 2 winner...")
        val decideWinner2Result = serviceGame.decideRoundWinner(game.id)
        assertIs<Either.Right<*>>(decideWinner2Result, "Failed to decide round 2 winner")
        val round2Winners = decideWinner2Result.value
        assertEquals(1, round2Winners.size, "Should have exactly 1 winner")
        assertEquals(player1User.name, round2Winners[0].name, "Player1 should be the winner")
        println("   Round 2 winner: ${round2Winners[0].name}")

        // Step 32: Distribute round 2 winnings
        println("\n32. Distributing round 2 winnings (pot = 100)...")
        val distributeWinnings2Result = serviceGame.distributeWinnings(game.id)
        assertIs<Either.Right<*>>(distributeWinnings2Result, "Failed to distribute round 2 winnings")
        println("   Winnings distributed")

        // Step 33: End game
        println("\n33. Ending game...")
        val endGameResult = serviceGame.endGame(game.id, clock.millis())
        assertIs<Either.Right<*>>(endGameResult, "Failed to end game")
        println("   Game ended")

        // Step 34: Check game winner
        println("\n34. Checking game winner...")
        val gameWinnerResult = serviceGame.decideGameWinner(game.id)
        assertIs<Either.Right<*>>(gameWinnerResult, "Failed to decide game winner")
        val gameWinners = gameWinnerResult.value

        println("\n=== GAME RESULTS ===")
        println("Game winners:")
        gameWinners.forEach { winner ->
            println("  - ${winner.name}: moneyWon = ${winner.moneyWon}")
        }

        // Assertions
        assertNotNull(gameWinners, "Game winners should not be null")
        assertTrue(gameWinners.isNotEmpty(), "There should be at least one game winner")

        // Find the winner(s) with the most money
        val maxMoneyWon = gameWinners.maxOf { it.moneyWon }
        val topWinners = gameWinners.filter { it.moneyWon == maxMoneyWon }

        println("\nTop winner(s):")
        topWinners.forEach { winner ->
            println("  - ${winner.name}: moneyWon = ${winner.moneyWon}")
        }

        // Verify money won is correct
        val hostWinner = gameWinners.find { it.name == hostUser.name }
        val player1Winner = gameWinners.find { it.name == player1User.name }

        assertNotNull(hostWinner, "Host should be in the winners list")
        assertNotNull(player1Winner, "Player1 should be in the winners list")

        // Host won round 1 (pot=50), Player1 won round 2 (pot=100)
        assertEquals(50, hostWinner.moneyWon, "Host should have won 50 (round 1)")
        assertEquals(100, player1Winner.moneyWon, "Player1 should have won 100 (round 2)")

        // Player1 should be the overall winner
        assertEquals(player1User.name, topWinners[0].name, "Player1 should be the overall winner with most money")
        assertEquals(100, topWinners[0].moneyWon, "Overall winner should have won 100")

        println("\n=== TEST PASSED ✓ ===")
        println("Winner correctly determined: ${topWinners[0].name} with ${topWinners[0].moneyWon} money won\n")
    }

    private fun playTurn(gameId: Int, playerId: Int, faceToLock: Face) {
        // Roll dice 5 times and lock the same face each time
        repeat(5) {
            // Roll dice
            val rollResult = serviceGame.rollDices(gameId, playerId)
            assertIs<Either.Right<*>>(rollResult, "Failed to roll dice")

            // Update turn with chosen dice
            val updateTurnResult = serviceGame.updateTurn(Dice(faceToLock), gameId)
            assertIs<Either.Right<*>>(updateTurnResult, "Failed to update turn")
        }

        // Move to next turn
        val nextTurnResult = serviceGame.nextTurn(gameId, playerId)
        assertIs<Either.Right<*>>(nextTurnResult, "Failed to move to next turn")
    }

    @Test
    fun `test winner determination with tied scores`() {
        println("\n=== Testing Winner Determination with Tied Scores ===\n")

        // Create and start game
        val createGameResult = serviceGame.createGame(clock.millis(), lobby.id, 2, hostUser.id)
        assertIs<Either.Right<*>>(createGameResult)
        val game = createGameResult.value

        val startGameResult = serviceGame.startGame(game.id, hostUser.id)
        assertIs<Either.Right<*>>(startGameResult)

        // Round 1: Host wins 50
        serviceGame.startNewRound(game.id)
        serviceGame.setAnte(game.id, 25, hostUser.id)
        serviceGame.payAnte(game.id)
        playTurn(game.id, hostUser.id, Face.ACE)
        playTurn(game.id, player1User.id, Face.NINE)
        serviceGame.decideRoundWinner(game.id)
        serviceGame.distributeWinnings(game.id)

        // Round 2: Player1 wins 50 (same amount)
        serviceGame.startNewRound(game.id)
        serviceGame.setAnte(game.id, 25, player1User.id)
        serviceGame.payAnte(game.id)
        playTurn(game.id, player1User.id, Face.KING)
        playTurn(game.id, hostUser.id, Face.NINE)
        serviceGame.decideRoundWinner(game.id)
        serviceGame.distributeWinnings(game.id)

        // End game and check for tie
        serviceGame.endGame(game.id, clock.millis())
        val gameWinnerResult = serviceGame.decideGameWinner(game.id)
        assertIs<Either.Right<*>>(gameWinnerResult)
        val gameWinners = gameWinnerResult.value

        println("Game winners (should be tied):")
        gameWinners.forEach { winner ->
            println("  - ${winner.name}: moneyWon = ${winner.moneyWon}")
        }

        // Both should have won 50
        assertEquals(2, gameWinners.size, "Both players should be winners (tied)")
        assertTrue(gameWinners.all { it.moneyWon == 50 }, "All winners should have won 50")

        println("\n=== TEST PASSED ✓ ===")
        println("Tie correctly detected: Both players won 50\n")
    }

    @Test
    fun `test error when trying to decide game winner before ending game`() {
        println("\n=== Testing Error When Deciding Winner Before Ending Game ===\n")

        // Create and start game
        val createGameResult = serviceGame.createGame(clock.millis(), lobby.id, 1, hostUser.id)
        assertIs<Either.Right<*>>(createGameResult)
        val game = createGameResult.value

        serviceGame.startGame(game.id, hostUser.id)

        // Play one round
        serviceGame.startNewRound(game.id)
        serviceGame.setAnte(game.id, 25, hostUser.id)
        serviceGame.payAnte(game.id)
        playTurn(game.id, hostUser.id, Face.ACE)
        playTurn(game.id, player1User.id, Face.NINE)
        serviceGame.decideRoundWinner(game.id)
        serviceGame.distributeWinnings(game.id)

        // Try to decide game winner WITHOUT ending game first
        val gameWinnerResult = serviceGame.decideGameWinner(game.id)

        assertIs<Either.Left<GameError>>(gameWinnerResult, "Should fail when game is not finished")
        assertEquals(GameError.GameNotFinished, gameWinnerResult.value, "Error should be GameNotFinished")

        println("Correctly returned error: ${gameWinnerResult.value}")
        println("\n=== TEST PASSED ✓ ===\n")
    }
}

