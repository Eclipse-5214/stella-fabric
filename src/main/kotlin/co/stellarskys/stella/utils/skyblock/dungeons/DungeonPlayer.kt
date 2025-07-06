package co.stellarskys.stella.utils.skyblock.dungeons

import kotlin.collections.mutableSetOf

class DungeonPlayer(val name: String) {
    var inRender: Boolean = false

    var iconX: Int? = null
    var iconZ: Int? = null
    var rotation: Float? = null
    var realX: Double? = null
    var realZ: Double? = null
    var currentRoom: Room? = null

    val visitedRooms = mutableSetOf<Room>() // replace with actual types if needed

    val clearedRooms = mutableMapOf(
        "WHITE" to mutableSetOf<Room>(), // using enum or some identifier instead of string
        "GREEN" to mutableSetOf<Room>()
    )

    var deaths: Int = 0

    var lastRoomCheck: Long? = null
    var lastRoom: Room? = null

    fun getGreenChecks(): MutableSet<Room> = clearedRooms["GREEN"] ?: mutableSetOf()
    fun getWhiteChecks(): MutableSet<Room> = clearedRooms["WHITE"] ?: mutableSetOf()

    override fun toString(): String {
        return "DungeonPlayer[iconX: $iconX, iconZ: $iconZ, rotation: $rotation, realX: $realX, realZ: $realZ, currentRoom: $currentRoom]"
    }
}
