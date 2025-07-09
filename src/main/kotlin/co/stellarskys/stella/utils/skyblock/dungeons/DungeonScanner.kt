package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.skyblock.LocationUtils

object DungeonScanner {
    val availableComponents = getScanCoords().toMutableList()
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Any?>(60) { null } // TODO: Replace Any? with Door class
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Any>() // TODO: Replace Any with Door class
    val players = mutableListOf<Any>() // TODO: Replace Any with DungeonPlayer class

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

        //checkDoorState()
        //checkPlayerState(ticks)

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

    init {
        println("[DungeonScanner] Initializing!")
        EventBus.register<AreaEvent.Main> ({
            TickUtils.schedule(2) {
                println("[DungeonScanner] World Changed! New world: ${LocationUtils.area}")
                if (LocationUtils.area != "catacombs") {
                    println("[DungeonScanner] not in catacombs!")
                    reset()
                }

                println("[DungeonScanner] Registering Scanner")
                tickRegister.register()
            }
        })
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

            // Door detection — skipped for now
            if (cx % 2 == 1 || cz % 2 == 1) {
                // TODO: door logic
                println("door stuff")
                continue
            }

            val x = cx / 2
            val z = cz / 2
            val idx = getRoomIdx(x to z)

            val room = Room(x to z, roofHeight).scan()
            println("[DungeonScanner] Added Room ${room.name}")
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

    fun getRoomAt(x: Int, z: Int): Room? {
        val comp = realCoordToComponent(x, z)
        val idx = getRoomIdx(comp)
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomIdx(comp: Pair<Int, Int>): Int = 6 * comp.second + comp.first

    fun getExploredRooms(): List<Room> {
        return rooms.filterNotNull().filter { it.explored }.distinct()
    }

    // TODO: tick logic for onWorldChange, onMapData, player state, checkmark updates, etc.
}
