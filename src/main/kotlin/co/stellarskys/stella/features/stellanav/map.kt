package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.GuiEvent
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.*
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.CommandUtils
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.*
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.math.RotationAxis
import java.awt.Color

@Stella.Module
object map: Feature("mapEnabled", area = "catacombs") {
    private const val name = "StellaNav"
    //val mapInfoUnder = config["mapInfoUnder"] as Boolean
    private val mapInfoUnder = true
    private val playerIcons = true
    private val defaultMapSize = Pair<Int, Int>(138, 138)
    private val roomSize = 18
    private val gapSize = 4
    private val spacing = roomSize + gapSize // 18 + 4 = 22

    private var mapLine1 = ""
    private var mapLine2 = ""

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        register<GuiEvent.HUD> { event ->
            if (HUDManager.isEnabled(name)) RenderMap(event.context)
        }

        register<TickEvent.Client> { event ->
            val dSecrets = "§7Secrets: " + "§b${Dungeon.secretsFound}§8-§e${Dungeon.scoreData.secretsRemaining}§8-§c${Dungeon.scoreData.totalSecrets}"
            val dCrypts = "§7Crypts: " + when {Dungeon.crypts >= 5 -> "§a${Dungeon.crypts}"; Dungeon.crypts > 0 -> "§e${Dungeon.crypts}"; else -> "§c0" }
            val dMimic = if (Dungeon.floorNumber in listOf(6, 7)) { "§7Mimic: " + if (Dungeon.mimicDead) "§a✔" else "§c✘" } else { "" }
            val minSecrets = "§7Min Secrets: " + if (Dungeon.secretsFound == 0) { "§b?" } else if (Dungeon.scoreData.minSecrets > Dungeon.secretsFound) { "§e${Dungeon.scoreData.minSecrets}" } else { "§a${Dungeon.scoreData.minSecrets}" }
            val dDeaths = "§7Deaths: " + if (Dungeon.teamDeaths < 0) { "§c${Dungeon.teamDeaths}" } else { "§a0" }
            val dScore = "§7Score: " + when {Dungeon.scoreData.score >= 300 -> "§a${Dungeon.scoreData.score}"; Dungeon.scoreData.score >= 270 -> "§e${Dungeon.scoreData.score}"; else -> "§c${Dungeon.scoreData.score}" } + if (Dungeon.hasPaul) " §b★" else ""

            mapLine1 = "$dSecrets    $dCrypts    $dMimic".trim()
            mapLine2 = "$minSecrets    $dDeaths    $dScore".trim()
        }
    }

    fun HUDEditorRender(context: DrawContext, x: Float, y: Float, width: Int, height: Int, scale: Float, partialTicks: Float, previewMode: Boolean){
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y, 0f)

        RenderMapBackground(context)
        RenderMapImage(context, true)

        if (mapInfoUnder) RenderInfoUnder(context, true)

        matrix.pop()
    }

    fun RenderMap(context: DrawContext) {
        val matrix = context.matrices
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        matrix.push()
        matrix.translate(x, y,0f)
        matrix.scale(scale, scale, 1f)

        RenderMapBackground(context)

        if(!Dungeon.inBoss()) {
            RenderMapImage(context, false)
            RenderCheckmarks(context)

            RenderPlayers(context)
        }

        if (mapInfoUnder) RenderInfoUnder(context, false)

        matrix.pop()
    }

    fun RenderMapImage(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices

        matrix.translate(5f,5f, 0f)
        if (preview) context.drawGuiTexture(RenderLayer::getGuiTextured, prevewMap, 0, 0, 128, 128)
        else {
            DungeonScanner.discoveredRooms.forEach { room ->
                if (room.room.explored) {
                    DungeonScanner.discoveredRooms.remove(room)
                }

                val color = Color(65 / 255f, 65 / 255f, 65 / 255f, 1f)

                val x = room.x * spacing
                val y = room.z * spacing
                val w = roomSize
                val h = roomSize

                Render2D.drawRect(context, x, y, w, h, color)
            }

            DungeonScanner.uniqueRooms.forEach { room ->
                if (room.explored) {
                    val color = roomTypeColors[room.type] ?: return@forEach

                    room.components.forEach { comp ->
                        val x = comp.first * spacing
                        val y = comp.second * spacing
                        val w = roomSize
                        val h = roomSize

                        //println("${room.name}, (${comp.first},${comp.second})")

                        Render2D.drawRect(context, x, y, w, h, color)
                    }

                    renderRoomConnectors(context, room)
                }
            }

            DungeonScanner.doors.forEach { door ->
                if (door?.state != DoorState.DISCOVERED) return@forEach

                val type = if (door.opened) DoorType.NORMAL else door.type
                val color = doorTypeColors[type] ?: return@forEach
                val comp = door.getComp()
                val cx = comp.first / 2  * 22
                val cy = comp.second / 2 * 22
                val vert = 0 == door.rotation
                val w = if (vert) 6 else 4
                val h = if (vert) 4 else 6

                val x = if (vert) cx + 6 else cx + 18
                val y = if (vert) cy + 18 else cy + 6

                Render2D.drawRect(context, x, y, w, h, color)
            }
        }
    }

    fun RenderCheckmarks(context: DrawContext) {
        DungeonScanner.discoveredRooms.forEach { room ->
            val w = 10
            val h = 12
            val x = room.x * spacing - w / 2 + roomSize / 2
            val y = room.z * spacing - h / 2 + roomSize / 2

            context.drawGuiTexture(RenderLayer::getGuiTextured , questionMark, x, y, w, h)
        }

        DungeonScanner.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach
            val checkmark = getCheckmarks(room.checkmark) ?: return@forEach

            val minX = room.components.minOf { it.first }
            val minZ = room.components.minOf { it.second }
            val maxX = room.components.maxOf { it.first }
            val maxZ = room.components.maxOf { it.second }

            val roomWidth = maxX - minX
            val roomHeight = maxZ - minZ

            var centerX = minX + roomWidth / 2.0
            var centerZ = minZ + roomHeight / 2.0

            if (room.shape == "L") {
                val topEdgeCount = room.components.count { it.second == minZ }
                centerZ += if (topEdgeCount == 2) -roomHeight / 2.0 else roomHeight / 2.0
            }

            val location = Pair(centerX, centerZ)

            val w = 12
            val h = 12

            val x = (location.first * spacing).toInt() - w / 2 + roomSize / 2
            val y = (location.second * spacing).toInt() - h / 2 + roomSize / 2

            context.drawGuiTexture(RenderLayer::getGuiTextured, checkmark, x, y, w, h)
        }
    }

    //nams and stuff

    fun RenderPlayers(context: DrawContext) {
        for ((k, v) in Dungeon.players) {
            val player = DungeonScanner.players.find { it.name == v.name } ?: continue
            val you = Stella.mc.player ?: continue
            if (v.className == "DEAD" && v.name != you.name.string) continue

            val iconX = player.iconX ?: continue
            val iconY = player.iconZ ?: continue
            val rotation = player.rotation ?: continue

            val x = (iconX / 125.0 * 128.0)
            val y = (iconY / 125.0 * 128.0)

            val matrix = context.matrices

            matrix.push()
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation))

            if (playerIcons) {
                val w = 12
                val h = 12

                val borderColor = getClassColor(v.className)

                Render2D.drawRect(context, (-w.toDouble() / 2.0).toInt(), (-h.toDouble() / 2.0).toInt(), w, h, borderColor)

                val scale = 1f - 0.2f

                matrix.scale(scale, scale, 1f)

                val color = -1

                context.drawTexture(
                    RenderLayer::getGuiTextured,                         // render layer provider
                    player.skin,
                    (-w.toDouble() / 2.0).toInt(),
                    (-h.toDouble() / 2.0).toInt(),
                    8f,
                    8f,
                    w,
                    h,
                    8,
                    8,
                    64,
                    64,
                )

                if (player.hat) {
                    context.drawTexture(
                        RenderLayer::getGuiTextured,                         // render layer provider
                        player.skin,
                        (-w.toDouble() / 2.0).toInt(),
                        (-h.toDouble() / 2.0).toInt(),
                        40f,
                        8f,
                        w,
                        h,
                        8,
                        8,
                        64,
                        64,
                    )
                }
            } else {
                val w = 7
                val h = 10
                val head = if (v.name == you.name.string) GreenMarker else WhiteMarker

                context.drawGuiTexture(
                    RenderLayer::getGuiTextured,
                    head,
                    (-w.toDouble() / 2.0).toInt(),
                    (-h.toDouble() / 2.0).toInt(),
                    w,
                    h
                )
            }

            matrix.pop()
        }
    }

    fun RenderInfoUnder(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices

        if (preview) {
            mapLine1 = "&7Secrets: &b?    &7Crypts: &c0    &7Mimic: &c✘";
            mapLine2 = "&7Min Secrets: &b?    &7Deaths: &a0    &7Score: &c0";
        }

        val w1 = mapLine1.width()
        val w2 = mapLine2.width()

        matrix.push()
        matrix.translate(128f / 2f, 130f, 0f)
        matrix.scale(0.6f, 0.6f, 1f)

        Render2D.drawString(context, mapLine1, -w1 / 2, 0)
        Render2D.drawString(context, mapLine2, -w2 / 2, 10)

        matrix.pop()
    }

    fun RenderMapBackground(context: DrawContext) {
        val matrix = context.matrices
        val w = defaultMapSize.first
        var h = defaultMapSize.second
        h += if (mapInfoUnder) 10 else 0

        matrix.translate(5f,5f,0f)
        Render2D.drawRect(context, 0, 0, w, h, Color(0,0,0, 100))
    }

    fun renderRoomConnectors(context: DrawContext, room: Room) {
        val spacing = roomSize + gapSize
        val connectorSize = gapSize

        val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))

        for ((x, z) in room.components) {
            for ((dx, dz) in directions) {
                val nx = x + dx
                val nz = z + dz

                //println("${room.name} component ($x,$z) checking at ($nx,$nz)")
                //println("does this pass? ${room.hasComponent(nx,nz)}")

                if (room.hasComponent(nx, nz)) {
                    val cx = (x + nx) / 2 * spacing
                    val cy = (z + nz) / 2 * spacing

                    val isVertical = dx == 0 // Z direction means vertical connector
                    val w = if (isVertical) roomSize else connectorSize
                    val h = if (isVertical) connectorSize else roomSize

                    val drawX = if (isVertical) cx else cx + roomSize
                    val drawY = if (isVertical) cy + roomSize else cy

                    Render2D.drawRect(
                        context,
                        drawX,
                        drawY,
                        w,
                        h,
                        roomTypeColors[room.type] ?: Color.GRAY
                    )
                }
            }
        }

        if (room.components.size == 4 && room.shape == "2x2") {
            val x = room.components[0].first * spacing + roomSize
            val y = room.components[0].second * spacing + roomSize
            val size = connectorSize

            Render2D.drawRect(
                context,
                 x, y,
                size,
                size,
                roomTypeColors[room.type] ?: Color.GRAY
            )
        }
    }
}

@Stella.Command
object DsDebug : CommandUtils(
    "samapdb"
) {
    override fun execute(context: CommandContext<FabricClientCommandSource>): Int {
        DungeonScanner.uniqueRooms.forEach { room ->
            ChatUtils.addMessage("${room.name} has a checkmark of ${room.checkmark}")
            ChatUtils.addMessage("${room.name} has ${room.components.size}")
        }

        ChatUtils.addMessage("discovered rooms has a size of ${DungeonScanner.discoveredRooms.size}")

        return 1
    }
}