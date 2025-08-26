package co.stellarskys.stella.features.stellanav.utils.render

import co.stellarskys.stella.features.stellanav.utils.mapConfig
import co.stellarskys.stella.features.stellanav.utils.mapConfig.mapInfoUnder
import co.stellarskys.stella.features.stellanav.utils.oscale
import co.stellarskys.stella.features.stellanav.utils.prevewMap
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import java.awt.Color
import java.awt.image.renderable.RenderContext

object mapRender {
    private val defaultMapSize = Pair(138, 138)

    fun render(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y,0f)
        matrix.scale(scale, scale, 1f)
        matrix.translate(5f,5f,0f)

        if(!Dungeon.inBoss() && !Dungeon.complete) {
            renderMapBackground(context)
            clear.renderMap(context)
            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapConfig.mapBorder) renderMapBorder(context)
        } else if (!Dungeon.complete && mapConfig.bossMapEnabled) {
            renderMapBackground(context)
            boss.renderMap(context)

            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapConfig.mapBorder) renderMapBorder(context)
        } else if (Dungeon.complete && mapConfig.scoreMapEnabled) {
            renderMapBackground(context)
            score.render(context)

            if (mapInfoUnder) renderInfoUnder(context, false)
            if (mapConfig.mapBorder) renderMapBorder(context)
        }

        matrix.pop()
    }

    fun renderPreview(context: DrawContext, x: Float, y: Float, scale: Float) {
        val matrix = context.matrices

        matrix.push()
        matrix.translate(x, y, 0f)

        renderMapBackground(context)
        context.drawGuiTexture(RenderLayer::getGuiTextured, prevewMap, 5, 5, 128, 128)

        if (mapInfoUnder) renderInfoUnder(context, true)

        matrix.pop()
    }

    fun renderInfoUnder(context: DrawContext, preview: Boolean) {
        val matrix = context.matrices

        var mapLine1 = Dungeon.mapLine1
        var mapLine2 = Dungeon.mapLine2

        if (preview) {
            mapLine1 = "&7Secrets: &b?    &7Crypts: &c0    &7Mimic: &c✘";
            mapLine2 = "&7Min Secrets: &b?    &7Deaths: &a0    &7Score: &c0";
        }

        val w1 = mapLine1.width()
        val w2 = mapLine2.width()

        matrix.push()
        matrix.translate(138f / 2f, 135f, 0f)
        matrix.scale(0.6f, 0.6f, 1f)

        Render2D.drawString(context, mapLine1, -w1 / 2, 0)
        Render2D.drawString(context, mapLine2, -w2 / 2, 10)

        matrix.pop()
    }


    fun renderMapBackground(context: DrawContext) {
        val w = defaultMapSize.first
        var h = defaultMapSize.second
        h += if (mapInfoUnder) 10 else 0

        Render2D.drawRect(context, 0, 0, w, h, mapConfig.mapBgColor)
    }

    fun renderMapBorder(context: DrawContext) {
        val (w, baseH) = defaultMapSize
        val borderWidth = mapConfig.mapBdWidth
        val h = baseH + if (mapInfoUnder) 10 else 0
        val color = mapConfig.mapBdColor
        // Top border
        Render2D.drawRect(context, -borderWidth, -borderWidth, w + borderWidth * 2, borderWidth, color)

        // Bottom border
        Render2D.drawRect(context, -borderWidth, h, w + borderWidth * 2, borderWidth, color)

        // Left border
        Render2D.drawRect(context, -borderWidth, 0, borderWidth, h, color)

        // Right border
        Render2D.drawRect(context, w, 0, borderWidth, h, color)
    }
}