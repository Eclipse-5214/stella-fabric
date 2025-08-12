package co.stellarskys.stella.features.stellanav.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.skyblock.dungeons.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.RoomType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.io.InputStreamReader

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

fun getTextColor(check: Checkmark?): String = when (check) {
    null -> "§7"
    Checkmark.WHITE -> "§f"
    Checkmark.GREEN -> "§a"
    Checkmark.FAILED -> "§c"
    else -> "§7"
}

val roomTypes = mapOf(
    63 to "Normal",
    30 to "Entrance",
    74 to "Yellow",
    18 to "Blood",
    66 to "Puzzle",
    62 to "Trap"
)

fun getClassColor(dClass: String?): Color = when (dClass) {
    "Healer" -> Color(240, 70, 240, 255)
    "Mage" -> Color(70, 210, 210, 255)
    "Berserk" -> Color(255, 0, 0, 255)
    "Archer" -> Color(30, 170, 50, 255)
    "Tank" -> Color(150, 150, 150, 255)
    else -> Color(0, 0, 0, 255)
}

val roomTypeColors = mapOf(
    RoomType.NORMAL to mapConfig.NormalColor,
    RoomType.PUZZLE to mapConfig.PuzzleColor,
    RoomType.TRAP to mapConfig.TrapColor,
    RoomType.YELLOW to mapConfig.MinibossColor,
    RoomType.BLOOD to mapConfig.BloodColor,
    RoomType.FAIRY to mapConfig.FaryColor,
    RoomType.ENTRANCE to mapConfig.EntranceColor,
)

val doorTypeColors = mapOf(
    DoorType.NORMAL to mapConfig.NormalDoorColor,
    DoorType.WITHER to mapConfig.WitherDoorColor,
    DoorType.BLOOD to mapConfig.BloodDoorColor,
    DoorType.ENTRANCE to mapConfig .EnteranceDoorColor
)


data class BossMapData(
    val image: String,
    val bounds: List<List<Double>>,
    val widthInWorld: Int,
    val heightInWorld: Int,
    val topLeftLocation: List<Int>,
    val renderSize: Int? = null
)

object BossMapRegistry {
    private val gson = Gson()
    private val bossMaps = mutableMapOf<String, List<BossMapData>>()

    init {
        val resourceManager = Stella.mc.resourceManager
        load(resourceManager)
    }

    fun load(resourceManager: ResourceManager) {
        val id = Identifier.of(Stella.NAMESPACE, "dungeons/imagedata.json")
        val optional = resourceManager.getResource(id)
        val resource = optional.orElse(null) ?: return

        val reader = InputStreamReader(resource.inputStream)
        val type = object : TypeToken<Map<String, List<BossMapData>>>() {}.type
        val parsed = gson.fromJson<Map<String, List<BossMapData>>>(reader, type)

        bossMaps.putAll(parsed)
    }

    fun getBossMap(floor: Int, playerPos: Vec3d): BossMapData? {
        val maps = bossMaps[floor.toString()] ?: return null
        return maps.firstOrNull { map ->
            (0..2).all { axis ->
                val min = map.bounds[0][axis]
                val max = map.bounds[1][axis]
                val p = listOf(playerPos.x, playerPos.y, playerPos.z)[axis]
                p in min..max
            }
        }
    }

    fun getAll(): Map<String, List<BossMapData>> = bossMaps
}
