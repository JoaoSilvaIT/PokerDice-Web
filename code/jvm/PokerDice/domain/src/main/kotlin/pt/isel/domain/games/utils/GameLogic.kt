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
    val numberOfHand = handIt.second.strength
    val numberOfMajorDice = handIt.first.dices.maxOf { it.face.strength }

    return numberOfHand + numberOfMajorDice
}

fun decideRoundWinner(round: Round): List<PlayerInGame> {
    // Filter out folded players from consideration
    val activeHands = round.playerHands.filterKeys { player ->
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
    when (char) {
        'A' -> Face.ACE
        'K' -> Face.KING
        'Q' -> Face.QUEEN
        'J' -> Face.JACK
        'T' -> Face.TEN
        else -> Face.NINE
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

/*
fun chooseDices(dicesToKeep: List<Dice>, turn: Turn): Turn {
    val newSetOfDices = dicesToKeep.toSet()
    val newTurn = turn.heldDice + newSetOfDices
    return turn.copy(heldDice = newTurn)
}

 */
