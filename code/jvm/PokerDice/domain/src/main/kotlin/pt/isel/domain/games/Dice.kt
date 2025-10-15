package pt.isel.domain.games

import pt.isel.utils.Face

data class Dice(
    val face: Face,
) {
    companion object {
        fun roll(): Dice {
            val faces = Face.entries.toTypedArray()
            val randomFace = faces.random()
            return Dice(randomFace)
        }
    }
}
