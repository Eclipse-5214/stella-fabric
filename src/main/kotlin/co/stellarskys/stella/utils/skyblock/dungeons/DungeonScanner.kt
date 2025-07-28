package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.novaconfig.utils.chatutils
import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.DungeonEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.CommandUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.LocationUtils
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.awt.Color
import java.util.UUID

fun MutableSet<DungeonPlayer>.pushCheck(player: DungeonPlayer) {
    if (player !in this) {
        add(player)
    }
}

fun MutableMap<Room, Long>.pushCheck(room: Room, defaultTime: Long = 0L) {
    this.putIfAbsent(room, defaultTime)
}


fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
    return when {
        n <= inMin -> outMin
        n >= inMax -> outMax
        else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}

object DungeonScanner {
    val availableComponents = getScanCoords().toMutableList()
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Any?>(60) { null } // TODO: Replace Any? with Door class
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>() // TODO: Replace Any with Door class
    val players = mutableListOf<DungeonPlayer>() // TODO: Replace Any with DungeonPlayer class

    var currentRoom: Room? = null
    var lastIdx: Int? = null

    // TODO: room enter/leave listener
    // val onRoomEnter = mutableListOf<(Room?) -> Unit>()
    // val onRoomLeave = mutableListOf<(Room?, Room?) -> Unit>()

    val tickRegister: EventBus.EventCall = EventBus.register<TickEvent.Client>({
        val player = Stella.mc.player ?: return@register
        if (LocationUtils.area != "catacombs") return@register

        val (x, z) = realCoordToComponent(player.x.toInt(), player.z.toInt())
        val idx = 6 * z + x

        // Scan dungeon
        scan()

        // Rotation + door + player state updates
        checkRoomState()
        checkDoorState()
        checkPlayerState(EventBus.totalTicks)

        // Bounds check: unregister if beyond 36-room limit
        if (idx > 35) {
            tickRegister.unregister()
            return@register
        }

        val prevRoom = lastIdx?.let { rooms[it] }
        val currRoom = rooms.getOrNull(idx)

        /*
        if (lastIdx != null && lastIdx != idx && prevRoom?.name != currRoom?.name) {
            _roomLeaveListener.forEach { it(currRoom, prevRoom) }
        }
         */

        if (lastIdx == idx) return@register


        if (prevRoom?.name != currRoom?.name) {
           // _roomEnterListener.forEach { it(currRoom) }
           println("Entered Room! ${currentRoom?.name}")
        }

        lastIdx = idx
        currentRoom = getRoomAt(player.x.toInt(), player.z.toInt())
    },false)

    val renderRooms = DungeonScanner.rooms.filterNotNull().map { room ->
        val comp = room.components.firstOrNull() ?: return@map null
        val color = roomTypeColors[room.type] ?: Color(0f, 0f, 0f, 1f)

        RoomRender(
            x = comp.first * 2,
            z = comp.second * 2,
            color = color,
            checkmark = room.checkmark,
            explored = room.explored,
            name = room.name
        )
    }.filterNotNull()

    val renderDoors = DungeonScanner.uniqueDoors.mapNotNull { door ->
        val color = doorTypeColors[door.type] ?: return@mapNotNull null
        DoorRender(
            x = door.getComp().first,
            z = door.getComp().second,
            rotation = door.rotation ?: 0,
            color = color,
            opened = door.opened
        )
    }


    data class RoomClearInfo(
        val time: Int,
        val room: Room,
        val solo: Boolean
    )

    data class RoomRender(
        val x: Int,
        val z: Int,
        val width: Int = 3,
        val height: Int = 3,
        val color: Color,
        val checkmark: Checkmark?,
        val explored: Boolean,
        val name: String?
    )

    data class DoorRender(
        val x: Int,
        val z: Int,
        val rotation: Int, // 0 = horizontal, 1 = vertical
        val color: Color,
        val opened: Boolean
    )



    init {
        //println("[DungeonScanner] Initializing!")
        EventBus.register<AreaEvent.Main> ({
            TickUtils.schedule(2) {
               // println("[DungeonScanner] World Changed! New world: ${LocationUtils.area}")
                if (LocationUtils.area != "catacombs") {
                    //println("[DungeonScanner] not in catacombs!")
                    reset()
                }

                //println("[DungeonScanner] Registering Scanner")
                tickRegister.register()
            }
        })

        EventBus.register<DungeonEvent.MapData>({ event ->
            for ((k, v) in Dungeon.icons) {
                val player = players.find { it.name == v.player }
                if (player == null || player.inRender) continue

                player.iconX = clampMap(v.x.toDouble() / 2 - Dungeon.mapCorners.first.toDouble(), 0.0, Dungeon.mapRoomSize.toDouble() * 6 + 20.0, 0.0, defaultMapSize.first.toDouble())
                player.iconZ = clampMap(v.y.toDouble() / 2 - Dungeon.mapCorners.second.toDouble(), 0.0, Dungeon.mapRoomSize.toDouble() * 6 + 20.0, 0.0, defaultMapSize.second.toDouble())
                player.realX = clampMap(player.iconX!!, 0.0, 125.0, -200.0, -10.0)
                player.realZ = clampMap(player.iconZ!!, 0.0, 125.0, -200.0, -10.0)
                player.rotation = v.yaw
                player.currentRoom = getRoomAt(player.realX!!.toInt(), player.realZ!!.toInt())
                player.currentRoom?.players?.pushCheck(player)

                //println(player.toString())
            }

            val colors = event.colors

            for (room in rooms) {
                if (room == null || room.components.isEmpty()) continue
                //println("cheaking room: ${room.name}")

                val (x, z) = room.components[0]
                val mx = Dungeon.mapCorners.first + Dungeon.mapRoomSize / 2 + Dungeon.mapGapSize * x
                val my = Dungeon.mapCorners.second + Dungeon.mapRoomSize / 2 + 1 + Dungeon.mapGapSize * z
                val idx = mx + my * 128

                val center = colors.getOrNull(idx - 1) ?: continue
                val rcolor = colors.getOrNull(idx + 5 + 128 * 4) ?: continue

                if (rcolor == 0.toByte() || rcolor == 85.toByte()) {
                    room.explored = false
                   // println("room not explored!")
                    continue
                }

                room.explored = true

                if (room.type == RoomType.NORMAL && room.height == null) {
                    room.loadFromMapColor(rcolor)
                    //println("loading from map!")
                }

                var check: Checkmark? = null
                when {
                    center == 30.toByte() && rcolor != 30.toByte() -> {
                        if (room.checkmark != Checkmark.GREEN) roomCleared(room, Checkmark.GREEN)
                        check = Checkmark.GREEN
                        //println("room fully cleared!")
                    }
                    center == 34.toByte() -> {
                        if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                        check = Checkmark.WHITE
                        //println("room cleared")
                    }
                    center == 18.toByte() && rcolor != 18.toByte() -> check = Checkmark.FAILED
                    room.checkmark == Checkmark.UNEXPLORED -> check = Checkmark.NONE
                }

                //println("final verdict $check")
                room.checkmark = check?: return@register

                //println("[MapStuff] Room ${room.name} has a checkmark of ${room.checkmark}")
            }

        })
    }

    fun onPlayerMove(entity: DungeonPlayer?, x: Double, z: Double, yaw: Float) {
        if (
            entity == null ||
            x !in -200.0..-10.0 ||
            z !in -200.0..-10.0
        ) return

        entity.inRender = true
        entity.iconX = clampMap(x, -200.0, -10.0, 0.0, defaultMapSize.first.toDouble())
        entity.iconZ = clampMap(z, -200.0, -10.0, 0.0, defaultMapSize.second.toDouble())

        entity.realX = x
        entity.realZ = z
        entity.rotation = yaw + 180f

        val currRoom = getRoomAt(x.toInt(), z.toInt())
        entity.currentRoom = currRoom
    }


    fun reset() {
        tickRegister.unregister()
        availableComponents.clear()
        availableComponents += getScanCoords()
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        currentRoom = null
        lastIdx = null
        players.clear()
    }

    fun scan() {
        if (availableComponents.isEmpty()) return

        for (idx in availableComponents.indices.reversed()) {
            val (cx, cz, rxz) = availableComponents[idx]
            val (rx, rz) = rxz
            if (!isChunkLoaded(rx,0,rz) || getHighestY(rx, rz) == null)  continue

            availableComponents.removeAt(idx)
            val roofHeight = getHighestY(rx, rz) ?: continue

            // Door detection
            if (cx % 2 == 1 || cz % 2 == 1) {
                if(roofHeight < 85){
                    val door = Door(rx to rz, cx to cz)

                    if (cz % 2 == 1) door.rotation = 0
                    addDoor(door)
                    //println("[DungeonScanner] Added door: ${door.type.toString()}")
                }

                continue
            }

            val x = cx / 2
            val z = cz / 2
            val idx = getRoomIdx(x to z)

            val room = Room(x to z, roofHeight).scan()
            //println("[DungeonScanner] Added Room ${room.name}")
            rooms[idx] = room
            uniqueRooms += room

            // Scan neighboring components
            for ((dx, dz, cxoff, zoff) in directions.map { list ->
                val (a, b, c, d) = list
                arrayOf(a, b, c, d)
            }) {
                val nx = rx + dx
                val nz = rz + dz

                val blockBelow = WorldUtils.getBlockNumericId(nx, roofHeight, nz)
                val blockAbove = WorldUtils.getBlockNumericId(nx, roofHeight + 1, nz)

                if (room.type == RoomType.ENTRANCE && blockBelow != 0) {
                    // TODO: check for entrance doors using a height-based test
                    continue
                }

                if (blockBelow != 0 || blockAbove == 0) continue

                val neighborComp = Pair(x + cxoff, z + zoff)
                val neighborIdx = getRoomIdx(neighborComp)
                if (neighborIdx !in rooms.indices) continue

                val neighborRoom = rooms[neighborIdx]

                if (neighborRoom == null) {
                    room.addComponent(neighborComp)
                    rooms[neighborIdx] = room
                } else if (neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE) {
                    mergeRooms(neighborRoom, room)
                }
            }
        }
    }

    fun getRoomAtIdx(idx: Int): Room? {
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomAtComp(comp: Pair<Int, Int>): Room? {
        val idx = getRoomIdx(comp)
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomAt(x: Int, z: Int): Room? {
        val comp = realCoordToComponent(x, z)
        val idx = getRoomIdx(comp)
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomIdx(comp: Pair<Int, Int>): Int = 6 * comp.second + comp.first

    fun getDoorIdx(comp: Pair<Int, Int>): Int {
        val base = ((comp.first - 1) shr 1) + 6 * comp.second
        return base - (base / 12)
    }

    fun getDoorAtIdx(idx: Int): Door? {
        return if (idx in doors.indices) doors[idx] as? Door else null
    }

    fun getDoorAtComp(comp: Pair<Int, Int>): Door? {
        val idx = getDoorIdx(comp)
        return getDoorAtIdx(idx)
    }

    fun getDoorAt(x: Int, z: Int): Door? {
        val comp = realCoordToComponent(x, z)
        return getDoorAtComp(comp)
    }

    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.getComp())
        if (idx !in doors.indices) return

        doors[idx] = door
        uniqueDoors += door
    }

    private fun mergeRooms(room1: Room, room2: Room) {
        uniqueRooms.remove(room2)
        for (comp in room2.components) {
            if (!room1.hasComponent(comp.first, comp.second)) {
                room1.addComponent(comp, update = false)
            }
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = room1
        }
        uniqueRooms += room1
        room1.update()
    }

    fun removeRoom(room: Room) {
        for (comp in room.components) {
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = null
        }
    }

    fun checkRoomState() {
        //println("checking room state")
        for (room in rooms) {
            if (room == null || room.rotation != null) continue
            room.findRotation()
        }
    }

    fun checkDoorState() {
        //println("[DungeonScanner] Checking door state")
        for (door in uniqueDoors) {
            if (door.opened) continue
            door.check()
        }
    }

    fun roomCleared(room: Room, check: Checkmark) {
        val players = room.players
        val isGreen = check == Checkmark.GREEN

        players.forEach {
            val v = it

            val colorKey = if (isGreen) "GREEN" else "WHITE"
            val clearedMap = v.clearedRooms[colorKey]

            clearedMap?.putIfAbsent(
                room.name!!,
                RoomClearInfo(
                    time = Dungeon.dungeonSeconds,
                    room = room,
                    solo = players.size == 1
                )
            )
        }
    }

    fun checkPlayerState(ticks: Int) {
        val world = Stella.mc.world ?: return

        if (players.size != Dungeon.partyMembers.size) return

        for (v in players) {
            val p = world.players.firstOrNull { it.name.string == v.name }
            val ping = Stella.mc.networkHandler?.getPlayerListEntry(p?.uuid ?: UUID(0, 0))?.latency ?: -1

            if (ticks != 0 && ticks % 4 == 0 && p != null) {
                if (ping != -1) {
                    v.inRender = true
                    onPlayerMove(v, p.x, p.z, p.yaw)
                } else {
                    v.inRender = false
                }
            }

            if (ping == -1) continue

            val currRoom = v.currentRoom ?: continue

            if (currRoom != v.lastRoom) {
                v.lastRoom?.players?.remove(v)
                currRoom.players.pushCheck(v)
            }

            v.visitedRooms.pushCheck(currRoom, 0)
            v.lastRoomCheck?.let {
                val timeSpent = System.currentTimeMillis() - it
                v.visitedRooms[currRoom] = (v.visitedRooms[currRoom] ?: 0) + timeSpent
            }

            v.lastRoomCheck = System.currentTimeMillis()
            v.lastRoom = currRoom
        }
    }

    fun getExploredRooms(): List<Room> {
        return rooms.filterNotNull().filter { it.explored }.distinct()
    }

}

@Stella.Command
object DsDebug : CommandUtils(
    "sadb"
) {
    override fun execute(context: CommandContext<FabricClientCommandSource>): Int {
        val room = DungeonScanner.currentRoom ?: return 0
        val name = room.name ?: "Unnamed"
        val cores = room.cores

        if (cores.isEmpty()) {
            ChatUtils.addMessage("${Stella.PREFIX} §b$name §fhas no scanned cores!")
        } else {
            ChatUtils.addMessage("${Stella.PREFIX} §b$name §fcore hash${if (cores.size > 1) "es" else ""}:")
            cores.forEach {
                ChatUtils.addMessage(" - $it")
            }
        }
        return 1
    }
}