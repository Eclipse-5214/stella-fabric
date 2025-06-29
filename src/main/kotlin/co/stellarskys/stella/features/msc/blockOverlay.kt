package co.stellarskys.stella.features.msc

import co.stellarskys.novaconfig.RGBA
import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.events.BlockOutlineEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import net.minecraft.block.ShapeContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexRendering
import net.minecraft.world.EmptyBlockView
import com.mojang.blaze3d.systems.RenderSystem

object blockoverlay : Feature("overlayEnabled") {
    override fun initialize() {
        register<BlockOutlineEvent> { event ->
            val blockPos = event.blockContext.blockPos()
            val consumers = event.WorldContext.consumers() ?: return@register
            val camera = mc.gameRenderer.camera
            val blockShape = event.blockContext.blockState().getOutlineShape(
                EmptyBlockView.INSTANCE, blockPos, ShapeContext.of(camera.focusedEntity)
            )
            if (blockShape.isEmpty) return@register

            val camPos = camera.pos
            RenderSystem.lineWidth((config["overlayLineWidth"] as Int).toFloat())
            event.cancel()

            VertexRendering.drawOutline(
                event.WorldContext.matrixStack(),
                consumers.getBuffer(RenderLayer.getLines()),
                blockShape,
                blockPos.x - camPos.x,
                blockPos.y - camPos.y,
                blockPos.z - camPos.z,
                (config["blockHighlightColor"] as RGBA).toColorInt()
            )
        }
    }
}