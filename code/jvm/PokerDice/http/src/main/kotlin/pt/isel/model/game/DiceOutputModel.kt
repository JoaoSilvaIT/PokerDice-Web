package pt.isel.model.game

import pt.isel.domain.games.Dice

data class DiceOutputModel(
    val currentDices: List<Dice>
)
