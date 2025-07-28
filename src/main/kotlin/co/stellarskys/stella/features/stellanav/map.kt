package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.DungeonEvent
import co.stellarskys.stella.events.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.mapRGBs
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.util.Identifier
import java.awt.Color
import java.awt.image.BufferedImage

@Stella.Module
object map: Feature("mapEnabled", area = "catacombs") {
    private const val name = "StellaNav"
    //val mapInfoUnder = config["mapInfoUnder"] as Boolean
    private val mapInfoUnder = false
    private val defaultMapSize = Pair<Int, Int>(138, 138)

    private val prevewMap = Identifier.of(Stella.NAMESPACE, "stellanav/defaultmap")
    private val roomRects = mutableListOf<RoomRect>()

    data class RoomRect(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val color: Color
    )

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        register<GuiEvent.HUD> { event ->
            if (HUDManager.isEnabled(name)) RenderMap(event.context, false)
        }

        register<DungeonEvent.MapData> { event ->
            if (Dungeon.inBoss()) return@register
        }
    }

    fun HUDEditorRender(context: DrawContext, x: Float, y: Float, width: Int, height: Int, scale: Float, partialTicks: Float, previewMode: Boolean){
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y, 0f)

        RenderMapBackground(context)
        RenderMapImage(context, true)

        matrix.pop()
    }

    fun RenderMap(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        matrix.push()
        matrix.translate(x, y,0f)
        matrix.scale(scale, scale, 1f)

        RenderMapBackground(context)
        RenderMapImage(context, false)
        matrix.pop()
    }

    fun RenderMapImage(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices

        matrix.translate(5f,5f, 0f)
        if (preview) context.drawGuiTexture({ RenderLayer.getGuiTextured(it) },prevewMap, 0, 0, 128, 128)
        else {
            roomRects.forEach { rect ->
                val x = rect.x
                val y = rect.y
                val w = rect.width
                val h = rect.height
                val color = rect.color

                Render2D.drawRect(context, x, y, w, h, color)
            }
        }
    }

    fun RenderMapBackground(context: DrawContext) {
        val matrix = context.matrices
        val w = defaultMapSize.first
        var h = defaultMapSize.second
        h += if (mapInfoUnder) 10 else 0

        matrix.translate(5f,5f,0f)
        Render2D.drawRect(context, 0, 0, w, h, Color(0,0,0, 100))
    }


}