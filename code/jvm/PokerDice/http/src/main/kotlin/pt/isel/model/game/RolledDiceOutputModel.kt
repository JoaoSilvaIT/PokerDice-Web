package pt.isel.model.game

import pt.isel.domain.games.Dice
import pt.isel.domain.games.utils.faceToChar

class RolledDiceOutputModel(diceRolled: List<Dice>) {
    val dice: List<String> = diceRolled.map { faceToChar(it.face).toString() }
}
