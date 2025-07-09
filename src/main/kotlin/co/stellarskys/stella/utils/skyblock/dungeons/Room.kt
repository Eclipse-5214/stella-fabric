package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.utils.WorldUtils
import net.minecraft.block.Blocks

data class RoomMetadata(
    val name: String,
    val type: String,
    val shape: String,
    val doors: String? = null,
    val secrets: Int = 0,
    val crypts: Int = 0,
    val reviveStones: Int = 0,
    val journals: Int = 0,
    val spiders: Boolean = false,
    val soul: Boolean = false,
    val cores: List<Int> = emptyList(),
    val id: List<String> = emptyList(),

    val secretDetails: SecretDetails = SecretDetails(),
    val secretCoords: Map<String, List<List<Int>>> = emptyMap()
)

data class SecretDetails(
    val wither: Int = 0,
    val redstoneKey: Int = 0,
    val bat: Int = 0,
    val item: Int = 0,
    val chest: Int = 0
)

class Room(
    initialComponent: Pair<Int, Int>,
    var height: Int? = null
) {
    val components = mutableListOf<Pair<Int, Int>>()
    val realComponents = mutableListOf<Pair<Int, Int>>()
    val cores = mutableListOf<Int>()

    var roomData: RoomMetadata? = null
    var shape: String = "1x1"
    var explored = false
    var checkmark = Checkmark.UNEXPLORED
    var players = mutableSetOf<String>()

    var name: String? = null
    var corner: Triple<Double, Double, Double>? = null
    var rotation: Int? = null
    var type: RoomType = RoomType.UNKNOWN
    var clear: ClearType? = null
    var secrets: Int = 0
    var crypts: Int = 0

    init {
        addComponents(listOf(initialComponent))
    }

    fun addComponent(comp: Pair<Int, Int>, update: Boolean = true): Room {
        if (!components.contains(comp)) components += comp
        if (update) update()
        return this
    }

    fun addComponents(comps: List<Pair<Int, Int>>): Room {
        comps.forEach { addComponent(it, update = false) }
        update()
        return this
    }

    fun hasComponent(x: Int, z: Int): Boolean {
        return components.any { it.first == x && it.second == z }
    }

    fun update() {
        components.sortWith(compareBy({ it.first }, { it.second }))
        realComponents.clear()
        realComponents += components.map { componentToRealCoords(it.first, it.second) }
        scan()
        shape = getRoomShape(components)
        corner = null
        rotation = null
    }

    fun scan(): Room {
        for ((x, z) in realComponents) {
            if (height == null) height = getHighestY(x, z)
            val core = getCore(x, z) ?: continue
            println("Room Core $core")
            cores += core
            loadFromCore(core)
            println("Loaded from core!")
        }
        return this
    }

    private fun loadFromCore(core: Int): Boolean {
        val data = RoomRegistry.getByCore(core) ?: return false
        loadFromData(data)
        return true
    }

    fun loadFromData(data: RoomMetadata) {
        roomData = data
        name = data.name
        type = roomTypeMap[data.type.lowercase()] ?: RoomType.NORMAL
        secrets = data.secrets
        crypts = data.crypts
        clear = when (data.type) {
            "mob" -> ClearType.MOB
            "miniboss" -> ClearType.MINIBOSS
            else -> null
        }

        println("[RoomLoader] Loading room metadata for: ${data.name}")
        println("  Type: ${data.type}")
        println("  Secrets: ${data.secrets}")
        println("  Crypts: ${data.crypts}")
    }

    fun loadFromMapColor(color: Int): Room {
        type = mapColorToRoomType[color] ?: RoomType.NORMAL
        when (type) {
            RoomType.BLOOD -> RoomRegistry.getAll().find { it.name == "Blood" }?.let { loadFromData(it) }
            RoomType.ENTRANCE -> RoomRegistry.getAll().find { it.name == "Entrance" }?.let { loadFromData(it) }
            else -> {}
        }
        return this
    }

    fun findRotation(): Room {
        if (height == null) return this

        if (type == RoomType.FAIRY) {
            rotation = 0
            val (x, z) = realComponents.first()
            corner = Triple(x - halfRoomSize + 0.5, height!!.toDouble(), z - halfRoomSize + 0.5)
            return this
        }

        val offsets = listOf(
            Pair(-halfRoomSize, -halfRoomSize),
            Pair(halfRoomSize, -halfRoomSize),
            Pair(halfRoomSize, halfRoomSize),
            Pair(-halfRoomSize, halfRoomSize)
        )

        for ((x, z) in realComponents) {
            for ((jdx, offset) in offsets.withIndex()) {
                val (dx, dz) = offset
                val nx = x + dx
                val nz = z + dz

                if (!isChunkLoaded(nx, height!!, nz)) continue
                val state = WorldUtils.getBlockStateAt(nx, height!!, nz) ?: continue
                if (state.isOf(Blocks.BLUE_TERRACOTTA)) {
                    rotation = jdx * 90
                    corner = Triple(nx + 0.5, height!!.toDouble(), nz + 0.5)
                    return this
                }
            }
        }
        return this
    }

    fun fromWorldPos(pos: Triple<Double, Double, Double>): Triple<Int, Int, Int>? {
        if (corner == null || rotation == null) return null
        val rel = Triple(
            (pos.first - corner!!.first).toInt(),
            (pos.second - corner!!.second).toInt(),
            (pos.third - corner!!.third).toInt()
        )
        return rotateCoords(rel, rotation!!)
    }

    fun toWorldPos(local: Triple<Int, Int, Int>): Triple<Double, Double, Double>? {
        if (corner == null || rotation == null) return null
        val rotated = rotateCoords(local, 360 - rotation!!)
        return Triple(
            rotated.first + corner!!.first,
            rotated.second + corner!!.second,
            rotated.third + corner!!.third
        )
    }

    fun getRoomCoord(pos: Triple<Double, Double, Double>) = fromWorldPos(pos)
    fun getRealCoord(local: Triple<Int, Int, Int>) = toWorldPos(local)
}