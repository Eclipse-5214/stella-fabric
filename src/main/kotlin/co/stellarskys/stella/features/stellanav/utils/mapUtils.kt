package co.stellarskys.stella.features.stellanav.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.skyblock.dungeons.Checkmark
import net.minecraft.util.Identifier
import java.awt.Color

fun oscale(floor: Int?): Float {
    if (floor == null) return 1f
    return when {
        floor == 0 -> 6f / 4f
        floor in 1..3 -> 6f / 5f
        else -> 1f
    }
}

val prevewMap = Identifier.of(Stella.NAMESPACE, "stellanav/defaultmap")

val greenCheck = Identifier.of(Stella.NAMESPACE, "stellanav/clear/bloommapgreencheck")
val whiteCheck =Identifier.of(Stella.NAMESPACE, "stellanav/clear/bloommapwhitecheck")
val failedRoom = Identifier.of(Stella.NAMESPACE, "stellanav/clear/bloommapfailedroom")
val questionMark = Identifier.of(Stella.NAMESPACE, "stellanav/clear/bloommapquestionmark")

val GreenMarker = Identifier.of(Stella.NAMESPACE, "stellanav/markerself")
val WhiteMarker = Identifier.of(Stella.NAMESPACE, "stellanav/markerother")

fun getCheckmarks(checkmark: Checkmark): Identifier? = when (checkmark) {
    Checkmark.GREEN -> greenCheck
    Checkmark.WHITE -> whiteCheck
    Checkmark.FAILED -> failedRoom
    Checkmark.UNEXPLORED -> questionMark
    else -> null
}


val mapRGBs = mapOf(
    18 to Color(1f, 0f, 0f, 1f),
    85 to Color(65 / 255f, 65 / 255f, 65 / 255f, 1f),
    30 to Color(20 / 255f, 133 / 255f, 0 / 255f, 1f),
    63 to Color(107 / 255f, 58 / 255f, 17 / 255f, 1f),
    82 to Color(224 / 255f, 0f, 255 / 255f, 1f),
    62 to Color(216 / 255f, 127 / 255f, 51 / 255f, 1f),
    74 to Color(254 / 255f, 223 / 255f, 0 / 255f, 1f),
    66 to Color(117 / 255f, 0f, 133 / 255f, 1f),
    119 to Color(0f, 0f, 0f, 1f)
)

fun getTextColor(check: Int?): String = when (check) {
    null -> "&7"
    1 -> "&f"
    2 -> "&a"
    3 -> "&c"
    else -> "&7"
}

val roomTypes = mapOf(
    63 to "Normal",
    30 to "Entrance",
    74 to "Yellow",
    18 to "Blood",
    66 to "Puzzle",
    62 to "Trap"
)

fun getClassColor(dClass: String?): List<Int> = when (dClass) {
    "Healer" -> listOf(240, 70, 240, 255)
    "Mage" -> listOf(70, 210, 210, 255)
    "Berserk" -> listOf(255, 0, 0, 255)
    "Archer" -> listOf(30, 170, 50, 255)
    "Tank" -> listOf(150, 150, 150, 255)
    else -> listOf(0, 0, 0, 255)
}

fun typeToName(type: Int): String? = when (type) {
    0 -> "NORMAL"
    1 -> "PUZZLE"
    2 -> "TRAP"
    3 -> "MINIBOSS"
    4 -> "BLOOD"
    5 -> "FAIRY"
    6 -> "RARE"
    7 -> "ENTRANCE"
    else -> null
}

fun typeToColor(type: Int): String? = when (type) {
    0 -> "7"
    1 -> "d"
    2 -> "6"
    3 -> "e"
    4 -> "c"
    5 -> "d"
    6 -> "b"
    7 -> "a"
    else -> null
}


