package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.ChatEvent
import co.stellarskys.stella.events.EntityEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.PacketEvent
import co.stellarskys.stella.events.ScoreboardEvent
import co.stellarskys.stella.events.TablistEvent
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.mixin.accessors.AccessorMapState
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.Utils.removeFormatting
import co.stellarskys.stella.utils.skyblock.LocationUtils
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapDecoration
import net.minecraft.item.map.MapDecorationTypes
import net.minecraft.item.map.MapState
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket
import net.minecraft.world.tick.Tick
import kotlin.math.ceil
import kotlin.math.floor

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

    val regexes = mapOf(
        // Strips unnecessary leading spaces, since input is trimmed
        "Floor" to Regex("""^The Catacombs \(([MF][1-7]|E)\)$"""),
        // Handles optional slot/class tags and captures name + class + floor (Roman numerals optional)
        "PlayerInfo" to Regex("""^\[(\d+)] (?:\[\w+] )?(\w{1,16})(?: .)? \((\w+)(?: ([IVXLCDM]+))?\)$"""),
        "SecretsFound" to Regex("""^Secrets Found: ([\d,.]+)$"""),
        "SecretsFoundPer" to Regex("""^Secrets Found: ([\d,.]+)%$"""),
        // More resilient to control chars, captures rank icon or grade
        "Milestone" to Regex("""^Your Milestone: .(.)$"""),
        "CompletedRooms" to Regex("""^Completed Rooms: (\d+)$"""),
        "TeamDeaths" to Regex("""^Team Deaths: (\d+)$"""),
        "PuzzleCount" to Regex("""^Puzzles: \((\d+)\)$"""),
        "Crypts" to Regex("""^Crypts: (\d+)$"""),
        "RoomSecretsFound" to Regex("""(\d+)/(\d+) Secrets"""),
        // Restructured to allow optional spaces/brackets/solver name
        "PuzzleState" to Regex("""^([\w ]+):\[([✦✔✖])]\s?\(?(\w{1,16})?\)?$"""),
        "OpenedRooms" to Regex("""^Opened Rooms: (\d+)$"""),
        "ClearedRooms" to Regex("""^Completed Rooms: (\d+)$"""),
        "ClearedPercent" to Regex("""^Cleared: (\d+)% \(\d+\)$"""),
        "DungeonTime" to Regex("""^Time: (?:(\d+)h)?\s?(?:(\d+)m)?\s?(?:(\d+)s)?$"""),
        "Mimic" to Regex("""^Party > (?:\[[\w+]+])?(?:\w{1,16}): (.*)$""")
    )



    var partyMembers: MutableList<String> = mutableListOf()
    var players: MutableMap<String, PlayerInfo> = mutableMapOf()
    var icons: MutableMap<String, Icon> = mutableMapOf() // Replace `Any` with actual icon type if known

    // floor
    var floor: String? = null
    var floorNumber: Int? = null

    // map
    val MapDecoration.mapX
        get() = (this.x + 128) shr 1

    val MapDecoration.mapZ
        get() = (this.z + 128) shr 1

    val MapDecoration.yaw
        get() = this.rotation * 22.5f

    //Map
    var mapCorners = Pair(5, 5)
    var mapRoomSize = 16
    var mapGapSize = 0
    var coordMultiplier = 0.625
    var mapData: MapState? = null
    var guessMapData: MapState? = null
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
    private var hasPaul = false

    // Mimic
    val MimicTrigger: EventBus.EventCall = EventBus.register<EntityEvent.Death>({ event ->
        val mcEntity = event.entity
        if (mcEntity !is ZombieEntity) return@register
        if (floorNumber !in listOf(6, 7) || mimicDead) return@register
        if (
            !mcEntity.isBaby ||
            EquipmentSlot.entries
                .filter { it.type == EquipmentSlot.Type.HUMANOID_ARMOR }
                .any { slot -> mcEntity.getEquippedStack(slot).isEmpty }
        ) return@register
        mimicDead = true
    }, false)

    init {
        EventBus.register<ScoreboardEvent.Clear>({ event ->
            val msg  = event.clear.trim()
            val percentMatch = regexes["ClearedPercent"]!!.find(msg)
            if ( percentMatch != null){
                val percentStr = percentMatch.groupValues[1]
                clearedPercent = percentStr.toInt()

                //println("[Dungeon] clear percentage: $clearedPercent")
                return@register
            }

            if (floor != null) return@register

            val floorMatch = regexes["Floor"]!!.find(msg) ?: return@register
            floor = floorMatch.groupValues[1]
            floorNumber = floor?.getOrNull(1)?.digitToIntOrNull() ?: 0
            secretsPercentNeeded = floorSecrets[floor] ?: 1.0
            MimicTrigger.register()

            //println("[Dungeon] floor: $floor, number: $floorNumber")
        })


        EventBus.register<TablistEvent.UpdatePlayer>({ event ->
            val msg = event.unformatted.trim()
            if(msg == "") return@register

            val timeMatch = regexes["DungeonTime"]!!.find(msg)
            if (timeMatch != null) {

                val hours = timeMatch.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
                val minutes = timeMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
                val seconds = timeMatch.groupValues.getOrNull(3)?.toIntOrNull() ?: 0

                dungeonSeconds = seconds + (minutes * 60) + (hours * 60 * 60)
                //println("[Dungeon] time: $dungeonSeconds")
            }

            secretsFound        = extractInt("SecretsFound", msg, secretsFound)
            secretsFoundPercent = extractDouble("SecretsFoundPer", msg, secretsFoundPercent)
            crypts              = extractInt("Crypts", msg, crypts)
            milestone           = extractString("Milestone", msg, milestone)
            completedRooms      = extractInt("CompletedRooms", msg, completedRooms)
            puzzleCount         = extractInt("PuzzleCount", msg, puzzleCount)
            teamDeaths          = extractInt("TeamDeaths", msg, teamDeaths)
            openedRooms         = extractInt("OpenedRooms", msg, openedRooms)
            clearedRooms        = extractInt("ClearedRooms", msg, clearedRooms)
            calculateScore()

            val puzzleMatch = regexes["PuzzleState"]!!.find(msg)
            if (puzzleMatch != null) {
                val puzzleName = puzzleMatch.groupValues[1]
                val puzzleState = puzzleMatch.groupValues[2]
                val failedBy = puzzleMatch.groupValues.getOrNull(3)

                val puzzleEnum = puzzleEnums[puzzleState]  // Assuming PuzzleEnums is a map<String, Int>
                if (puzzleEnum == 1) puzzlesDone++

                //println("[Dungeon] Puzzle: $puzzleName, State: $puzzleState")
            }

            val playerMatch = regexes["PlayerInfo"]?.find(msg)?.groupValues
            if (playerMatch != null) {
                val playerName = playerMatch[2]
                val className = playerMatch[3]
                val classLevel = playerMatch[4]

                //println("[Dungeon] Player: $playerName, Class: $className, Level: ${decodeRoman(classLevel)}")

                if (!partyMembers.contains(playerName)) {
                    partyMembers.add(playerName)
                }

                if (className.isNotEmpty()) {
                    players[playerName] = PlayerInfo(
                        className = className,
                        level = decodeRoman(classLevel),
                        levelRoman = classLevel,
                        name = playerName
                    )

                    val player = Stella.mc.player ?: return@register
                    if (playerName.equals(player.name.string, ignoreCase = true)) {
                        println("[Dungeon] This is you")
                        currentClass = className
                        currentLevel = decodeRoman(classLevel)
                    }
                }
            }



        })

        EventBus.register<PacketEvent.Received>({ event ->
            if (event.packet is MapUpdateS2CPacket && mapData == null) {
                val world = Stella.mc.world ?: return@register
                val id = event.packet.mapId.id
                if (id and 1000 == 0) {
                    val guess = FilledMapItem.getMapState(event.packet.mapId, world) ?: return@register
                    if(guess.decorations.any {it.type == MapDecorationTypes.FRAME }) {
                        guessMapData = guess
                    }
                }
            }
        })

        EventBus.register<TickEvent.Client>({ event ->
            if (!calibrated) {
                if (mapData == null) {
                    mapData = getCurrentMapState()
                }

                calibrated = calibrateDungeonMap()
            } else if (!inBoss()) {
                (mapData ?: guessMapData)?.let {
                    updatePlayersFromMap(it)
                    //DungeonScanner.updateRoomsFromMap(it)
                    DungeonScanner.scanFromMap(it)
                    checkBloodDone(it)
                }
            }
        })

        EventBus.register<ChatEvent.Receive>( { event ->
            if (floorNumber !in listOf(6, 7) || floor == null) return@register

            val msg = event.message.string.removeFormatting()
            val match = regexes["Mimic"]!!.matchEntire(msg)

            if (match == null) return@register
            if (mimicMessages.none { it == match.groupValues[1].lowercase() }) return@register
            mimicDead = true
        })

        EventBus.register<AreaEvent.Main> ({
            TickUtils.schedule(2) {
                if (LocationUtils.area != "catacombs") reset()
            }
        })
    }

    fun reset() {
        partyMembers = mutableListOf()
        players.clear()
        icons.clear()
        mapCorners = Pair(5, 5)
        mapRoomSize = 16
        mapGapSize = 0
        mapData = null
        guessMapData = null
        calibrated = false
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
        MimicTrigger.unregister()
    }

    data class ScoreData(
        var totalSecrets: Int = 0,
        var secretsRemaining: Int = 0,
        var totalRooms: Int = 0,
        var deathPenalty: Int = 0,
        var completionRatio: Double = 0.0,
        var adjustedRooms: Int = 0,
        var roomsScore: Double = 0.0,
        var skillScore: Double = 0.0,
        var secretsScore: Double = 0.0,
        var exploreScore: Double = 0.0,
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

    data class Icon(
        val x: Int,
        val y: Int,
        val yaw: Float,
        val player: String?
    )

    // functions
    fun calculateScore() {
        if (floor == null) return

        val missingPuzzles = puzzleCount - puzzlesDone

        scoreData.totalSecrets = ((100.0 / secretsFoundPercent) * secretsFound + 0.5).toInt()
        scoreData.secretsRemaining = scoreData.totalSecrets - secretsFound

        val estimatedTotal = ((100.0 / clearedPercent) * completedRooms + 0.4)
        val totalRooms = estimatedTotal.toInt().takeIf { it > 0 } ?: 36
        scoreData.totalRooms = totalRooms

        //println("[asdfasdf] total rooms: ${scoreData.totalRooms}")

        scoreData.adjustedRooms = completedRooms

        if (!bloodDone || !inBoss()) {
            //println("adjusting for blood")
            scoreData.adjustedRooms++
        }
        if (completedRooms <= scoreData.totalRooms - 1 && !bloodDone) scoreData.adjustedRooms++

        scoreData.deathPenalty = (teamDeaths * -2) + if (hasSpiritPet && teamDeaths > 0) 1 else 0

        scoreData.completionRatio = scoreData.adjustedRooms.toDouble() / scoreData.totalRooms
        scoreData.roomsScore = (80 * scoreData.completionRatio).coerceIn(0.0, 80.0)
        scoreData.skillScore = (20 + scoreData.roomsScore - 10 * missingPuzzles + scoreData.deathPenalty).coerceIn(20.0, 100.0)

        scoreData.secretsScore = (40 * ((secretsFoundPercent / 100.0) / secretsPercentNeeded)).coerceIn(0.0, 40.0)
        scoreData.exploreScore = (60 * scoreData.completionRatio + scoreData.secretsScore).coerceIn(0.0, 100.0)

        if (clearedPercent == 0) scoreData.exploreScore = 0.0

        val cryptScore = crypts.coerceAtMost(5)
        val mimicScore = if (mimicDead) 2 else 0
        val paulScore = if (hasPaul) 10 else 0

        scoreData.bonusScore = cryptScore + mimicScore + paulScore

        val totalTime = dungeonSeconds - (floorTimes[floor] ?: 0)
        val speedScore = when {
            totalTime < 480 -> 100.0
            totalTime <= 600 -> 140 - totalTime / 12.0
            totalTime <= 840 -> 115 - totalTime / 24.0
            totalTime <= 1140 -> 108 - totalTime / 30.0
            totalTime <= 3940 -> 98.5 - totalTime / 40.0
            else -> 0.0
        }

        scoreData.score = (scoreData.skillScore + scoreData.exploreScore + speedScore + scoreData.bonusScore).toInt()
        scoreData.maxSecrets = ceil(scoreData.totalSecrets * secretsPercentNeeded).toInt()
        scoreData.minSecrets = floor(scoreData.maxSecrets * ((40.0 - scoreData.bonusScore + scoreData.deathPenalty) / 40.0)).toInt()

        //println("[Dungeon] Score: ${scoreData.score}, Min Secs: ${scoreData.minSecrets}, Max Secs: ${scoreData.maxSecrets}")
        //println("Skill: ${scoreData.skillScore}, Explore: ${scoreData.exploreScore}")
        //("Speed: $speedScore, Bonus: ${scoreData.bonusScore}")

        /*
        if (scoreData.score >= 300 && !_has300Triggered) {
            _on300Listeners.forEach { it() }
            _has300Triggered = true
            return
        }
        if (scoreData.score < 270 || _has270Triggered) return

        _on270Listeners.forEach { it() }
        _has270Triggered = true
        */
    }

    fun inBoss(): Boolean{
        if (floor == null) return false
        val player = Stella.mc.player ?: return false

        val (x, z) = realCoordToComponent(player.x.toInt(), player.z.toInt())
        val idx = 6 * z + x

        return idx > 35
    }


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

    fun updatePlayersFromMap(state: MapState) {
        var iconOrder = partyMembers.toMutableList()

        // Rotate: move first to the end
        iconOrder.add(iconOrder.removeAt(0))

        // Filter out "DEAD" players — assuming name contains "DEAD"
        iconOrder = iconOrder.filterNot { it.contains("DEAD", ignoreCase = true) }.toMutableList()

        // Bail if no one left
        if (iconOrder.isEmpty()) return

        state as AccessorMapState
        state.decorationsMap.forEach { iconName, decoration ->
            val match = Regex("^icon-(\\d+)$").find(iconName) ?: return@forEach
            val iconNumber = match.groupValues[1].toInt()
            val player = if (iconNumber < iconOrder.size) iconOrder[iconNumber] else null

            //println("[MapStuff] $player, X: ${decoration.mapX}, y: ${decoration.mapZ}, Yaw: ${decoration.yaw}")

            icons[iconName] = Icon(
                x = decoration.mapX,
                y = decoration.mapZ,
                yaw = decoration.yaw + 180f,
                player = player
            )
        }

        DungeonScanner.updatePlayersFromMap()
    }

    fun checkBloodDone(state: MapState) {
        if (bloodDone) return

        val startX = mapCorners.first + (mapRoomSize / 2)
        val startY = mapCorners.second + (mapRoomSize / 2) + 1

        for (x in startX until 118 step (mapGapSize / 2)) {
            for (y in startY until 118 step (mapGapSize / 2)) {
                val i = x + y * 128
                if ((i % 1).toDouble() != 0.0) break // Kotlin uses Double for mod check like JS
                if (state.colors.getOrNull(i) == null) continue

                val center = state.colors[i - 1]
                val roomColor = state.colors.getOrNull(i + 5 + 128 * 4) ?: continue

                if (roomColor != 18.toByte()) continue
                //bloodOpen = true

                if (center != 30.toByte()) continue
                bloodDone = true
            }
        }
    }
    //internal helpers
    private fun extractInt(key: String, msg: String, fallback: Int): Int {
        val match = regexes[key]!!.find(msg) ?: return fallback
        return match.groupValues.getOrNull(1)?.replace(",", "")?.toIntOrNull() ?: fallback
    }

    private fun extractDouble(key: String, msg: String, fallback: Double): Double {
        val match = regexes[key]!!.find(msg) ?: return fallback
        return match.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: fallback
    }

    private fun extractString(key: String, msg: String, fallback: String): String {
        val match = regexes[key]!!.find(msg) ?: return fallback
        return match.groupValues.getOrNull(1) ?: fallback
    }

    // Usefull functions
    fun getMilestone(asIndex: Boolean = false): Any =
        if (asIndex) milestones.indexOf(milestone) else milestone

    fun getByClass(className: String): List<PlayerInfo> {
        return players.values.filter { it.className == className }
    }

    fun getByName(playerName: String): PlayerInfo? {
        return players[playerName]
    }

    fun isDupeClass(className: String): Boolean {
        return getByClass(className).size > 1
    }

    fun getMageReduction(cooldown: Double, checkClass: Boolean = false): Double {
        if (checkClass && currentClass != "Mage") return cooldown

        val mult = if (isDupeClass("Mage")) 1 else 2
        return cooldown * (0.75 - (floor(currentLevel / 2.0) / 100.0) * mult)
    }

    fun setSpiritPet(value: Boolean) {
        hasSpiritPet = value
    }
}