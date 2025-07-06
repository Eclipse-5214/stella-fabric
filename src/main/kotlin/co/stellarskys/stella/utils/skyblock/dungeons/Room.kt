package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.WorldUtils
import com.google.gson.JsonObject
import net.minecraft.block.Blocks
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import kotlin.math.floor


class Room(
    comp: Any, // Replace `Any` with actual type of `comp`
    var height: Int?
) {
    private val comps = mutableListOf<Pair<Int, Int>>()
    private val rcomps = mutableListOf<Pair<Int, Int>>()
    private var cores = mutableListOf<Any>()
    private val roomsJson = getRoomData()

    var explored: Boolean = false
    var name: String? = null
    var corner: List<Double>? = null
    var rotation: Int? = null
    var type: RoomType = RoomType.UNKNOWN
    var clear: ClearType? = null
    var roomData: JsonObject? = null
    var shape: String = "1x1"
    var secrets: Int = 0
    var crypts: Int = 0
    var checkmark: Checkmark = Checkmark.UNEXPLORED
    val players = mutableSetOf<DungeonPlayer>()

    fun loadFromData(roomData: JsonObject) {
        this.roomData = roomData

        this.name = roomData.get("name")?.asString
        val typeKey = roomData.get("type")?.asString
        this.type = typeKey?.let { RoomType.fromString(it) } ?: RoomType.NORMAL

        this.secrets = roomData.get("secrets")?.asInt ?: 0

        val coresJson = roomData.getAsJsonArray("cores")
        this.cores = coresJson?.mapNotNull { it.asString }?.toMutableList() ?: mutableListOf()

        val clearType = roomData.get("clear")?.asString
        this.clear = when (clearType) {
            "mob" -> ClearType.MOB
            "miniboss" -> ClearType.MINIBOSS
            else -> null
        }

        this.crypts = roomData.get("crypts")?.asInt ?: 0
    }

    fun loadFromCore(core: Int): Boolean {
        val roomsArray = roomsJson?.getAsJsonArray("rooms") ?: return false

        for (entry in roomsArray) {
            val obj = entry.asJsonObject
            val cores = obj.getAsJsonArray("cores")?.mapNotNull { it.asString } ?: continue
            if (core.toString() in cores) {
                loadFromData(obj)
                return true
            }
        }
        return false
    }

    fun loadFromMapColor(color: Int): Room {
        type = MapColorToRoomType[color] ?: RoomType.NORMAL

        if (type == RoomType.BLOOD) {
            val data = roomsJson?.getAsJsonArray("rooms")?.find {
                it.asJsonObject.get("name")?.asString == "Blood"
            }?.asJsonObject

            data?.let { loadFromData(it) }
        }

        if (type == RoomType.ENTRANCE) {
            val data = roomsJson?.getAsJsonArray("rooms")?.find {
                it.asJsonObject.get("name")?.asString == "Entrance"
            }?.asJsonObject

            data?.let { loadFromData(it) }
        }

        return this
    }

    fun scan():Room {
        for ((x, z) in rcomps) {
            if (height == null) {
                height = getHighestY(x, z)
            }
            val core = getCore(x, z)
            loadFromCore(core) // assumes roomsJson is passed or available
        }

        return this
    }

    fun hasComponent(x: Int, z: Int): Boolean =
        comps.any { (cx, cz) -> cx == x && cz == z }

    fun addComponent(comp: Pair<Int, Int>, update: Boolean = true): Room {
        if (hasComponent(comp.first, comp.second)) return this
        comps.add(comp)
        if (update) update()
        return this
    }

    fun addComponents(newComps: List<Pair<Int, Int>>): Room {
        newComps.forEach { addComponent(it, update = false) }
        update()
        return this
    }

    fun update() {
        comps.sortWith(compareBy({ it.first }, { it.second }))
        rcomps.clear()
        rcomps.addAll(comps.map { componentToRealCoords(it, false) })
        scan()
        shape = getRoomShape(comps)
        corner = null
        rotation = null
    }

    fun findRotation() {
        val y = height ?: return

        if (type == RoomType.FAIRY) {
            rotation = 0
            val (x, z) = rcomps.firstOrNull() ?: return
            corner = listOf(x - halfRoomSize + 0.5, y.toDouble(), z - halfRoomSize + 0.5)
            return
        }

        for ((x, z) in rcomps) {
            for ((jdx, offset) in directions.withIndex()) {
                val dx = offset[0]
                val dz = offset[2]
                val nx = x + dx
                val nz = z + dz

                if (!isChunkLoaded(nx, y, nz)) return

                val state = WorldUtils.getBlockStateAt(nx, y,nz) ?: continue
                val isBlueTerracotta = state.isOf(Blocks.BLUE_TERRACOTTA)

                if (!isBlueTerracotta) continue

                rotation = jdx * 90
                corner = listOf(nx + 0.5, y.toDouble(), nz + 0.5)
                return
            }
        }
    }


    fun fromPos(pos: List<Double>): List<Double>? {
        val rot = rotation ?: return null
        val c = corner ?: return null
        val rel = pos.mapIndexed { idx, v -> floor(v - c[idx]) }
        return rotateCoords(rel, rot)
    }

    fun fromComp(comp: List<Double>): List<Double>? {
        val rot = rotation ?: return null
        val c = corner ?: return null
        val rotated = rotateCoords(comp, (360 - rot) % 360)
        return rotated.mapIndexed { idx, v -> floor(v + c[idx]) }
    }


    fun getRoomCoord(pos: List<Double>) = fromPos(pos)

    fun getRealCoord(comp: List<Double>) = fromComp(comp)
}