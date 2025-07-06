package co.stellarskys.stella.features.msc

import co.stellarskys.novaconfig.RGBA
import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.events.BlockOutlineEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.RenderHelper
import co.stellarskys.stella.utils.render.StellaRenderLayers
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.ShapeContext
import net.minecraft.client.render.*
import net.minecraft.world.EmptyBlockView
import java.awt.Color

object blockOverlay : Feature("overlayEnabled") {
    override fun initialize() {
        register<BlockOutlineEvent> { event ->
            val blockPos = event.blockContext.blockPos()
            val consumers = event.WorldContext.consumers() ?: return@register
            val camera = mc.gameRenderer.camera
            val blockShape = event.blockContext.blockState().getOutlineShape(EmptyBlockView.INSTANCE, blockPos, ShapeContext.of(camera.focusedEntity))
            if (blockShape.isEmpty) return@register

            val camPos = camera.pos
            val outlineColor: RGBA = config["blockHighlightColor"] as RGBA
            val outlineWidth: Float = (config["overlayLineWidth"] as Int).toFloat()

            VertexRendering.drawOutline(
                event.WorldContext.matrixStack(),
                consumers.getBuffer(StellaRenderLayers.getChromaLines(5.0)),
                blockShape,
                blockPos.x - camPos.x,
                blockPos.y - camPos.y,
                blockPos.z - camPos.z,
                outlineColor.toColorInt()
            )
        }
    }
}