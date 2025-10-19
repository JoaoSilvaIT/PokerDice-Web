package pt.isel.utils

import pt.isel.domain.games.Hand
import pt.isel.domain.games.Round
import pt.isel.domain.users.User

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

fun calculateFullHandValue(handit: Pair<Hand, HandRank>): Int {
    val numberOfHand = handit.second.strength
    val numberOfMajorDice = handit.first.dices.maxOf { it.face.strength }

    return numberOfHand + numberOfMajorDice
}

fun decideRoundWinner(round: Round): List<User> {
    val userHandValues: List<Pair<User, Int>> =
        round.userHands.map { (user, hand) ->
            val handRank = defineHandRank(hand)
            val handValue = calculateFullHandValue(handRank)
            Pair(user, handValue)
        }

    val winnerValue = userHandValues.maxOf { it.second }

    return userHandValues.filter { it.second == winnerValue }.map { it.first }
}

fun distributeWinnings(winners: List<User>, pot: Int): List<User> {
    val winningsPerWinner = pot / winners.size
    return winners.map { winner ->
        winner.copy(balance = winner.balance + winningsPerWinner)
    }
}
