package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.LegIDs
import co.stellarskys.stella.utils.WorldUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.util.Identifier
import net.minecraft.resource.ResourceManager
import net.minecraft.util.math.BlockPos
import java.awt.Color
import java.io.InputStreamReader

object RoomRegistry {
    private val gson = Gson()
    private val byCore = mutableMapOf<Int, RoomMetadata>()
    private val allRooms = mutableListOf<RoomMetadata>()

    init {
        // Automatically load room data from resource manager when object initializes
        val resourceManager = Stella.mc.resourceManager
        load(resourceManager)
    }

    fun load(resourceManager: ResourceManager) {
        val id = Identifier.of(Stella.NAMESPACE, "dungeons/roomdata.json")
        val optional = resourceManager.getResource(id)
        val resource = optional.orElse(null) ?: return

        val reader = InputStreamReader(resource.inputStream)
        val type = object : TypeToken<List<RoomMetadata>>() {}.type
        val rooms = gson.fromJson<List<RoomMetadata>>(reader, type)
        allRooms += rooms

        for (room in rooms) {
            for (core in room.cores) {
                byCore[core] = room
            }
        }
    }

    fun getByCore(core: Int): RoomMetadata? = byCore[core]
    fun getAll(): List<RoomMetadata> = allRooms
}

// Dungeon grid constants
val cornerStart = Pair(-200, -200)
val cornerEnd   = Pair(-10, -10)

const val dungeonRoomSize = 31
const val dungeonDoorSize = 1
const val roomDoorCombinedSize = dungeonRoomSize + dungeonDoorSize
const val halfRoomSize = dungeonRoomSize / 2
const val halfCombinedSize = roomDoorCombinedSize / 2

val directions = listOf(
    listOf(halfCombinedSize, 0, 1, 0),
    listOf(-halfCombinedSize, 0, -1, 0),
    listOf(0, halfCombinedSize, 0, 1),
    listOf(0, -halfCombinedSize, 0, -1)
)

val mapDirections = listOf(
    1 to 0,  // East
    -1 to 0, // West
    0 to 1,  // South
    0 to -1  // North
)

val defaultMapSize = Pair(125, 125)

val blacklist = setOf(5, 54, 146)

fun getCore(x: Int, z: Int): Int {
    val sb = StringBuilder(150)
    val chunk = Stella.mc.world!!.getChunk(x shr 4, z shr 4)
    val height = getHighestY(x, z)?.coerceIn(11..140) ?: 140 .coerceIn(11..140)

    sb.append(CharArray(140 - height) { '0' })
    var bedrock = 0

    for (y in height downTo 12) {
        val blockState = chunk.getBlockState(BlockPos(x, y, z))
        val id = if (blockState.isAir) 0 else LegIDs.getLegacyId(blockState)

        if (id == 0 && bedrock >= 2 && y < 69) {
            sb.append(CharArray(y - 11) { '0' })
            break
        }

        if (id == 7) {
            bedrock++
        } else {
            bedrock = 0
            if (id in blacklist) continue
        }
        sb.append(id)
    }
    return sb.toString().hashCode()
}

fun getHighestY(x: Int, z: Int): Int? {
    for (y in 255 downTo 0) {
        val id = WorldUtils.getBlockNumericId(x, y, z)
        if (id != 0 && id != 41) return y
    }
    return null
}

fun componentToRealCoords(x: Int, z: Int, includeDoors: Boolean = false): Pair<Int, Int> {
    val (x0, z0) = cornerStart
    val offset = if (includeDoors) halfCombinedSize else roomDoorCombinedSize
    return Pair(x0 + halfRoomSize + offset * x, z0 + halfRoomSize + offset * z)
}

fun realCoordToComponent(x: Int, z: Int, includeDoors: Boolean = false): Pair<Int, Int> {
    val (x0, z0) = cornerStart
    val size = if (includeDoors) halfCombinedSize else roomDoorCombinedSize
    val shift = 4 + ((size - 16) shr 4)
    return Pair(((x - x0 + 0.5).toInt() shr shift), ((z - z0 + 0.5).toInt() shr shift))
}

fun rotateCoords(pos: Triple<Int, Int, Int>, degree: Int): Triple<Int, Int, Int> {
    val d = (degree + 360) % 360
    return when (d) {
        0   -> pos
        90  -> Triple(pos.third, pos.second, -pos.first)
        180 -> Triple(-pos.first, pos.second, -pos.third)
        270 -> Triple(-pos.third, pos.second, pos.first)
        else -> pos
    }
}

fun getRoomShape(comps: List<Pair<Int, Int>>): String {
    val count = comps.size
    val xs = comps.map { it.first }.toSet()
    val zs = comps.map { it.second }.toSet()

    return when {
        count == 1 -> "1x1"
        count == 2 -> "1x2"
        count == 3 -> if (xs.size == 3 || zs.size == 3) "1x3" else "L"
        count == 4 -> if (xs.size == 1 || zs.size == 1) "1x4" else "2x2"
        else       -> "Unknown"
    }
}

enum class DoorType { NORMAL, WITHER, BLOOD, ENTRANCE }
enum class DoorState { UNDISCOVERED, DISCOVERED }
enum class ClearType { MOB, MINIBOSS }
enum class Checkmark { NONE, WHITE, GREEN, FAILED, UNEXPLORED, UNDISCOVERED }

enum class RoomType {
    NORMAL, PUZZLE, TRAP, YELLOW, BLOOD, FAIRY, RARE, ENTRANCE, UNKNOWN;
}



val roomTypeMap = mapOf(
    "mobs" to RoomType.NORMAL,
    "miniboss" to RoomType.NORMAL,
    "puzzle" to RoomType.PUZZLE,
    "trap" to RoomType.TRAP,
    "gold" to RoomType.YELLOW,
    "blood" to RoomType.BLOOD,
    "fairy" to RoomType.FAIRY,
    "rare" to RoomType.RARE,
    "spawn" to RoomType.ENTRANCE
)


val mapColorToRoomType = mapOf(
    18 to RoomType.BLOOD,
    30 to RoomType.ENTRANCE,
    63 to RoomType.NORMAL,
    82 to RoomType.FAIRY,
    62 to RoomType.TRAP,
    74 to RoomType.YELLOW,
    66 to RoomType.PUZZLE
)

fun getScanCoords(): List<Triple<Int, Int, Pair<Int, Int>>> {
    val coords = mutableListOf<Triple<Int, Int, Pair<Int, Int>>>()

    for (z in 0..<11) {
        for (x in 0..<11) {
            if (x % 2 == 1 && z % 2 == 1) continue

            val rx = cornerStart.first + halfRoomSize + x * halfCombinedSize
            val rz = cornerStart.second + halfRoomSize + z * halfCombinedSize
            coords += Triple(x, z, Pair(rx, rz))
        }
    }

    return coords
}

fun isChunkLoaded(x: Int, y: Int, z: Int): Boolean {
    val world = Stella.mc.world ?: return false
    val chunkX = x shr 4
    val chunkZ = z shr 4
    return world.chunkManager.isChunkLoaded(chunkX, chunkZ)
}

fun decodeRoman(roman: String): Int {
    val values = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000
    )

    var total = 0
    var prev = 0

    for (char in roman.uppercase()) {
        val value = values[char] ?: return 0  // return 0 for invalid characters
        if (value > prev) {
            total += value - 2 * prev  // correct for subtraction rule
        } else {
            total += value
        }
        prev = value
    }

    return total
}