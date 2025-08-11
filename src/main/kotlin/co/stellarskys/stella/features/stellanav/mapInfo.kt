package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.GuiEvent
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.render.mapRender
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.client.gui.DrawContext
import kotlin.collections.contains

@Stella.Module
object mapInfo: Feature("seperateMapInfo", "catacombs") {
    const val name = "Map Info"

    override fun initialize() {
        HUDManager.registerCustom(name, 200, 30,this::HUDEditorRender)

        register<GuiEvent.HUD> { event ->
            if (HUDManager.isEnabled(name)) RenderNormal(event.context)
        }
    }

    fun HUDEditorRender(context: DrawContext, x: Float, y: Float, width: Int, height: Int, scale: Float, partialTicks: Float, previewMode: Boolean){
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y, 0f)

        RenderMapInfo(context, true)

        matrix.pop()
    }

    fun RenderNormal(context: DrawContext) {
        val matrix = context.matrices
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        matrix.push()
        matrix.translate(x, y,0f)
        matrix.scale(scale, scale, 1f)

        RenderMapInfo(context, false)

        matrix.pop()
    }

    fun RenderMapInfo(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices

        var mapLine1 = Dungeon.mapLine1
        var mapLine2 = Dungeon.mapLine2

        if (preview) {
            mapLine1 = "§7Secrets: §b?    §7Crypts: §c0    §7Mimic: §c✘";
            mapLine2 = "§7Min Secrets: §b?    §7Deaths: §a0    §7Score: §c0";
        }
        val w1 = mapLine1.width()
        val w2 = mapLine2.width()

        matrix.push()
        matrix.translate( 100f, 5f, 0f)

        Render2D.drawString(context,mapLine1,-w1 / 2, 0)
        Render2D.drawString(context,mapLine2,-w2 / 2, 10)

        matrix.pop()
    }
}