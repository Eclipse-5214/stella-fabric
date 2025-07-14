package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.ScoreboardEvent
import co.stellarskys.stella.events.TablistEvent
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.utils.ScoreboardUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.skyblock.LocationUtils
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner.tickRegister
import co.stellarskys.stella.utils.stripControlCodes
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapState
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.jvm.optionals.getOrNull

val puzzleEnums = mapOf(
    "✦" to 0,
    "✔" to 1,
    "✖" to 2
)

val milestones = listOf("⓿", "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾")

val floorSecrets = mapOf(
    "F1" to 0.3,
    "F2" to 0.4,
    "F3" to 0.5,
    "F4" to 0.6,
    "F5" to 0.7,
    "F6" to 0.85
)

val floorTimes = mapOf(
    "F3" to 120,
    "F4" to 240,
    "F5" to 120,
    "F6" to 240,
    "F7" to 360,
    "M1" to 0,
    "M2" to 0,
    "M3" to 0,
    "M4" to 0,
    "M5" to 0,
    "M6" to 120,
    "M7" to 360
)

val mimicMessages = listOf(
    "mimic dead",
    "mimic dead!",
    "mimic killed",
    "mimic killed!",
    "\$skytils-dungeon-score-mimic\$"
)


object Dungeon {
    // regex
    val floorRegex = Regex("""^The Catacombs \(([MF][1-7]|E)\)$""")
    val clearPercentRegex = Regex("""^Cleared: (\d+)% \(\d+\)$""")


    var partyMembers: List<String> = emptyList()
    var players: MutableMap<String, PlayerInfo> = mutableMapOf()
    var icons: MutableMap<String, Any> = mutableMapOf() // Replace `Any` with actual icon type if known

    // floor
    var floor: String? = null
    var floorNumber: Int? = null

    // map
    var mapData = null
    var mapCorners = Pair(5, 5)
    var mapRoomSize = 16
    var mapGapSize = 0
    var coordMultiplier = 0.625
    var calibrated = false

    // dungeon
    var secretsFound: Int = 0
    var secretsFoundPercent: Double = 0.0
    var crypts: Int = 0
    var milestone: String = "⓿"
    var completedRooms: Int = 0
    var puzzleCount: Int = 0
    var teamDeaths: Int = 0
    var openedRooms: Int = 0
    var clearedRooms: Int = 0

    // player
    var currentClass: String? = null
    var currentLevel: Int = 0
    var puzzlesDone: Int = 0
    var clearedPercent: Int = 0
    var secretsPercentNeeded: Double = 1.0

    // score
    var scoreData = ScoreData()
    var bloodDone: Boolean = false
    var dungeonSeconds: Int = 0
    var hasSpiritPet: Boolean = false
    var mimicDead: Boolean = false
    var has270Triggered: Boolean = false
    var has300Triggered: Boolean = false


    init {
        EventBus.register<ScoreboardEvent.Clear>({ event ->
            val msg  = event.clear.trim()
            val percentMatch = clearPercentRegex.find(msg)
            if ( percentMatch != null){
                val percentStr = percentMatch.groupValues[1]
                clearedPercent = percentStr.toInt()

                println("[Dungeon] clear percentage: $clearedPercent")
                return@register
            }

            if (floor != null) return@register

            val match = floorRegex.find(msg) ?: return@register
            floor = match.groupValues[1]
            floorNumber = floor?.getOrNull(1)?.digitToIntOrNull() ?: 0
            println("[Dungeon] floor: $floor, number: $floorNumber")
        })


        EventBus.register<TablistEvent.UpdatePlayer>({ event ->

        })

        EventBus.register<AreaEvent.Main> ({
            TickUtils.schedule(2) {
                if (LocationUtils.area != "catacombs") reset()
            }
        })
    }

    fun reset() {
        partyMembers = emptyList()
        players.clear()
        icons.clear()
        mapData = null
        mapCorners = Pair(5, 5)
        mapRoomSize = 16
        mapGapSize = 0
        floor = null
        floorNumber = null
        secretsFound = 0
        secretsFoundPercent = 0.0
        crypts = 0
        milestone = "⓿"
        completedRooms = 0
        puzzleCount = 0
        teamDeaths = 0
        openedRooms = 0
        clearedRooms = 0
        currentClass = null
        currentLevel = 0
        puzzlesDone = 0
        clearedPercent = 0
        secretsPercentNeeded = 1.0
        scoreData = ScoreData()
        bloodDone = false
        dungeonSeconds = 0
        hasSpiritPet = false
        mimicDead = false
        has270Triggered = false
        has300Triggered = false
    }

    data class ScoreData(
        var totalSecrets: Int = 0,
        var secretsRemaining: Int = 0,
        var totalRooms: Int = 0,
        var deathPenalty: Int = 0,
        var completionRatio: Double = 0.0,
        var adjustedRooms: Int = 0,
        var roomsScore: Int = 0,
        var skillScore: Int = 0,
        var secretsScore: Int = 0,
        var exploreScore: Int = 0,
        var bonusScore: Int = 0,
        var score: Int = 0,
        var maxSecrets: Int = 0,
        var minSecrets: Int = 0
    )

    data class PlayerInfo(
        val className: String,
        val level: Int,
        val levelRoman: String,
        val name: String
    )


    // map stuff
    fun getCurrentMapState(): MapState? {
        val stack = Stella.mc.player?.inventory?.getStack(8) ?: return null
        if (stack.item !is FilledMapItem || !stack.name.string.contains("Magical Map")) return null
        return FilledMapItem.getMapState(stack, Stella.mc.world!!)
    }

    fun calibrateDungeonMap(): Boolean {
        val mapState = getCurrentMapState() ?: return false
        val entranceInfo = findEntranceCorner(mapState.colors) ?: return false

        val (startIndex, size) = entranceInfo
        mapRoomSize = size
        mapGapSize = mapRoomSize + 4 // compute gap size from room width

        var x = (startIndex % 128) % mapGapSize
        var z = (startIndex / 128) % mapGapSize

        val floor = floorNumber?: return false
        if (floor in listOf(0, 1)) x += mapGapSize
        if (floor == 0) z += mapGapSize

        mapCorners = x to z
        coordMultiplier = mapGapSize / roomDoorCombinedSize.toDouble()

        println("[MapCalib] roomSize=$mapRoomSize, gapSize=$mapGapSize, corners=$mapCorners, multiplier=$coordMultiplier")
        return true
    }

    fun findEntranceCorner(colors: ByteArray): Pair<Int, Int>? {
        for (i in colors.indices) {
            if (colors[i] != 30.toByte()) continue

            // Check horizontal 15-block chain
            if (i + 15 < colors.size && colors[i + 15] == 30.toByte()) {
                // Check vertical 15-block chain
                if (i + 128 * 15 < colors.size && colors[i + 128 * 15] == 30.toByte()) {
                    var length = 0
                    while (i + length < colors.size && colors[i + length] == 30.toByte()) {
                        length++
                    }
                    return Pair(i, length)
                }
            }
        }
        return null
    }
}