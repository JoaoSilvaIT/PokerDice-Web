package pt.isel.domain.games.utils

import pt.isel.domain.games.Dice
import pt.isel.domain.games.Hand
import pt.isel.domain.games.PlayerInGame
import pt.isel.domain.games.Round

fun defineHandRank(hand: Hand): Pair<Hand, HandRank> {
    val equalDice = hand.dices.groupingBy { it }.eachCount()

    val rank =
        when (equalDice.entries.size) {
            1 -> HandRank.FIVE_OF_A_KIND
            2 -> if (equalDice.values.any { it > 3 }) HandRank.FOUR_OF_A_KIND else HandRank.FULL_HOUSE
            3 -> if (equalDice.values.any { it == 3 }) HandRank.THREE_OF_A_KIND else HandRank.TWO_PAIR
            4 -> HandRank.ONE_PAIR
            else -> if (equalDice.entries.any { it.key.face == Face.NINE }) HandRank.HIGH_DICE else HandRank.STRAIGHT
        }

    return Pair(hand, rank)
}

fun calculateFullHandValue(handIt: Pair<Hand, HandRank>): Int {
    val hand = handIt.first
    val rank = handIt.second
    val rankStrength = rank.strength

    // Group dice by face and count occurrences
    val diceGroups = hand.dices.groupingBy { it.face }.eachCount()

    // Calculate the primary value based on the dice that form the combination
    val (primaryDiceValue, kicker) =
        when (rank) {
            HandRank.FIVE_OF_A_KIND -> {
                diceGroups.keys.first().strength to 0
            }
            HandRank.FOUR_OF_A_KIND -> {
                val main = diceGroups.entries.first { it.value == 4 }.key.strength
                val kick = diceGroups.entries.first { it.value == 1 }.key.strength
                main to kick
            }
            HandRank.FULL_HOUSE -> {
                val triplet = diceGroups.entries.first { it.value == 3 }.key.strength
                val pair = diceGroups.entries.first { it.value == 2 }.key.strength
                triplet to pair
            }
            HandRank.THREE_OF_A_KIND -> {
                val main = diceGroups.entries.first { it.value == 3 }.key.strength
                val kick = diceGroups.entries.filter { it.value == 1 }.maxOf { it.key.strength }
                main to kick
            }
            HandRank.TWO_PAIR -> {
                val pairs = diceGroups.entries.filter { it.value == 2 }.map { it.key.strength }.sortedDescending()
                val kick = diceGroups.entries.firstOrNull { it.value == 1 }?.key?.strength ?: 0
                pairs[0] * 10 + pairs[1] to kick
            }
            HandRank.ONE_PAIR -> {
                val main = diceGroups.entries.first { it.value == 2 }.key.strength
                val kick = diceGroups.entries.filter { it.value == 1 }.maxOf { it.key.strength }
                main to kick
            }
            HandRank.STRAIGHT, HandRank.HIGH_DICE -> {
                val sorted = hand.dices.map { it.face.strength }.sortedDescending()
                sorted[0] to sorted[1]
            }
        }

    // rank * 1000 + primaryValue * 10 + kicker
    return rankStrength * 1000 + primaryDiceValue * 10 + kicker
}

fun decideRoundWinner(round: Round): List<PlayerInGame> {
    // Filter out folded players from consideration
    val activeHands =
        round.playerHands.filterKeys { player ->
            !round.foldedPlayers.any { it.id == player.id }
        }

    if (activeHands.isEmpty()) {
        // Fallback if everyone folded (should be handled by fold logic, but for safety)
        return emptyList()
    }

    val userHandValues: List<Pair<PlayerInGame, Int>> =
        activeHands.map { (user, hand) ->
            val handRank = defineHandRank(hand)
            val handValue = calculateFullHandValue(handRank)
            Pair(user, handValue)
        }

    if (userHandValues.isEmpty()) return emptyList()

    val winnerValue = userHandValues.maxOf { it.second }

    return userHandValues.filter { it.second == winnerValue }.map { it.first }
}

fun decideGameWinner(players: List<PlayerInGame>): List<PlayerInGame> {
    val maxWinnings = players.maxOf { it.moneyWon }
    return players.filter { it.moneyWon == maxWinnings }
}

fun roll(): Dice {
    val faces = Face.entries.toTypedArray()
    val randomFace = faces.random()
    return Dice(randomFace)
}

fun rollDicesLogic(numberOfDices: Int): List<Dice> {
    val dices = mutableListOf<Dice>()
    repeat(numberOfDices) { dices.add(roll()) }
    return dices
}

fun lockDices(dices: List<Dice>): Hand {
    require(dices.size == 5) { "Unexpected number of dice, must be 5 dices." }
    return Hand(dices)
}

fun charToFace(char: Char): Face =
    when (char.uppercaseChar()) {
        'A' -> Face.ACE
        'K' -> Face.KING
        'Q' -> Face.QUEEN
        'J' -> Face.JACK
        'T' -> Face.TEN
        '9' -> Face.NINE
        else -> throw IllegalArgumentException("Unknown dice face character: '$char'")
    }

fun faceToChar(face: Face): Char =
    when (face) {
        Face.ACE -> 'A'
        Face.KING -> 'K'
        Face.QUEEN -> 'Q'
        Face.JACK -> 'J'
        Face.TEN -> 'T'
        Face.NINE -> '9'
    }
