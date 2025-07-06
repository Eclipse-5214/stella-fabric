package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.WorldUtils
import net.minecraft.util.Identifier
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.io.InputStreamReader
import kotlin.math.floor

fun getRoomData(): JsonObject? {
    val roomData = Identifier.of(Stella.NAMESPACE, "/dungeons/roomdata.json")

    val resourceManager = Stella.mc.resourceManager

    return try {
        resourceManager.getResource(roomData).get().inputStream.use { stream ->
            val reader = InputStreamReader(stream)
            JsonParser.parseReader(reader).asJsonObject
        }
    } catch (e: Exception) {
        null
    }
}

val cornerStart = Pair(-200,-200)
val cornerEnd = Pair(-10,-10)

const val dungeonRoomSize = 31
const val dungeonDoorSize = 1
const val roomDoorCombinedSize = dungeonRoomSize + dungeonDoorSize

val halfRoomSize = floor(dungeonRoomSize.toDouble() / 2).toInt()
val halfCombinedSize = floor(roomDoorCombinedSize.toDouble() / 2).toInt()

val directions = listOf(
    listOf(halfCombinedSize, 0, 1, 0),
    listOf(-halfCombinedSize, 0, -1, 0),
    listOf(0, halfCombinedSize, 0, 1),
    listOf(0, -halfCombinedSize, 0, -1)
)

val defaultMapSize = Pair(125, 125)

fun getHighestY(x: Int, z: Int): Int {
    for (y in 256 downTo 1) {
        val block = WorldUtils.getBlockAt(x, y, z)
        val id = Registries.BLOCK.getRawId(block)
        if (id == 0 || id == 41) continue
        return y
    }
    return 0
}

fun hashCode(str: String): Int =
    str.fold(0) { acc, c -> (acc shl 5) - acc + c.code } and 0xFFFFFFFF.toInt()

fun getScanCoords(): List<List<Int>> {
    val coords = mutableListOf<List<Int>>()
    for (z in 0..<11) {
        for (x in 0..<11) {
            if (x % 2 == 1 && z % 2 == 1) continue
            val rx = cornerStart.first + halfRoomSize + x * halfCombinedSize
            val rz = cornerStart.second + halfRoomSize + z * halfCombinedSize
            coords.add(listOf(x, z, rx, rz))
        }
    }
    return coords
}

fun getCore(x: Int, z: Int): Int {
    val ids = buildString {
        for (y in 140 downTo 12) {
            val block = WorldUtils.getBlockAt(x, y, z)
            val id = Registries.BLOCK.getRawId(block)
            append(if (id == 101 || id == 54) "0" else id)
        }
    }
    return hashCode(ids)
}

fun componentToRealCoords(coord: Pair<Int, Int>, includeDoors: Boolean = false): Pair<Int, Int> {
    val (x, z) = coord
    val (x0, z0) = cornerStart
    return if (includeDoors) {
        Pair(
            x0 + halfRoomSize + halfCombinedSize * x,
            z0 + halfRoomSize + halfCombinedSize * z
        )
    } else {
        Pair(
            x0 + halfRoomSize + roomDoorCombinedSize * x,
            z0 + halfRoomSize + roomDoorCombinedSize * z
        )
    }
}

fun isChunkLoaded(x: Int, y: Int, z: Int): Boolean {
    val world = Stella.mc.world ?: return false
    val chunkPos = BlockPos(x, y, z)
    return world.getChunk(chunkPos) != null
}

fun realCoordToComponent(coord: Pair<Int, Int>, includeDoors: Boolean = false): Pair<Int, Int> {
    val (x, z) = coord
    val (x0, z0) = cornerStart
    val size = if (includeDoors) halfCombinedSize else roomDoorCombinedSize
    val s = 4 + ((size - 16) shr 4)

    return Pair(
        ((x - x0 + 0.5).toInt()) shr s,
        ((z - z0 + 0.5).toInt()) shr s
    )
}

enum class DoorType(val id: Int) {
    NORMAL(0),
    WITHER(1),
    BLOOD(2),
    ENTRANCE(3);
}

enum class ClearType(val id: Int) {
    MOB(0),
    MINIBOSS(1);
}

enum class Checkmark(val id: Int) {
    NONE(0),
    WHITE(1),
    GREEN(2),
    FAILED(3),
    UNEXPLORED(4);
}

enum class RoomType(val id: Int) {
    NORMAL(0),
    PUZZLE(1),
    TRAP(2),
    YELLOW(3),
    BLOOD(4),
    FAIRY(5),
    RARE(6),
    ENTRANCE(7),
    UNKNOWN(8);

    companion object {
        private val nameMap = mapOf(
            "normal" to NORMAL,
            "puzzle" to PUZZLE,
            "trap" to TRAP,
            "yellow" to YELLOW,
            "blood" to BLOOD,
            "fairy" to FAIRY,
            "rare" to RARE,
            "entrance" to ENTRANCE
        )

        fun fromString(name: String): RoomType =
            nameMap[name.lowercase()] ?: UNKNOWN
    }
}

fun rotateCoords(pos: List<Double>, degree: Int): List<Double> {
    val (x, y, z) = pos
    val normalized = (degree % 360 + 360) % 360

    return when (normalized) {
        0 -> listOf(x, y, z)
        90 -> listOf(z, y, -x)
        180 -> listOf(-x, y, -z)
        270 -> listOf(-z, y, x)
        else -> listOf(x, y, z) // fallback if angle is unexpected
    }
}

fun getRoomShape(components: List<Pair<Int, Int>>?): String {
    if (components.isNullOrEmpty() || components.size > 4) return "Unknown"

    val xs = components.map { it.first }.toSet()
    val zs = components.map { it.second }.toSet()

    return when (components.size) {
        1 -> "1x1"
        2 -> "1x2"
        4 -> if (xs.size == 1 || zs.size == 1) "1x4" else "2x2"
        3 -> if (xs.size == 3 || zs.size == 3) "1x3" else "L"
        else -> "Unknown"
    }
}

val MapColorToRoomType: Map<Int, RoomType> = mapOf(
    18 to RoomType.BLOOD,
    30 to RoomType.ENTRANCE,
    63 to RoomType.NORMAL,
    82 to RoomType.FAIRY,
    62 to RoomType.TRAP,
    74 to RoomType.YELLOW,
    66 to RoomType.PUZZLE
)