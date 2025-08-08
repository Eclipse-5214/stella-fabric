package co.stellarskys.stella.features.msc

import co.stellarskys.stella.Stella
import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.events.RenderEvent.BlockOutline
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.render.StellaRenderLayers
import net.minecraft.block.ShapeContext
import net.minecraft.client.render.*
import net.minecraft.world.EmptyBlockView

@Stella.Module
object blockOverlay : Feature("overlayEnabled") {
    override fun initialize() {
        register<BlockOutline> { event ->
            val blockPos = event.blockContext.blockPos()
            val consumers = event.worldContext.consumers() ?: return@register
            val camera = mc.gameRenderer.camera
            val blockShape = event.blockContext.blockState().getOutlineShape(EmptyBlockView.INSTANCE, blockPos, ShapeContext.of(camera.focusedEntity))
            if (blockShape.isEmpty) return@register

            val camPos = camera.pos

            val chroma = config["chromaHighlight"] as Boolean

            val outlineColor = config["blockHighlightColor"] as RGBA
            val outlineWidth = (config["overlayLineWidth"] as Int).toFloat()

            val fillColor = config["blockFillColor"] as RGBA

            event.cancel()

            val x = blockPos.x - camPos.x
            val y = blockPos.y - camPos.y
            val z = blockPos.z - camPos.z

            VertexRendering.drawOutline(
                event.worldContext.matrixStack(),
                consumers.getBuffer(if(chroma) StellaRenderLayers.getChromaLines(5.0) else StellaRenderLayers.getLines(5.0)),
                blockShape,
                x, y, z,
                outlineColor.toColorInt()
            )

            if (config["fillBlockOverlay"] as Boolean) {
                val (rRaw, gRaw, bRaw, aRaw) = fillColor
                val r = rRaw / 255f
                val g = gRaw / 255f
                val b = bRaw / 255f
                val a = aRaw / 255f

                blockShape.forEachBox { minX, minY, minZ, maxX, maxY, maxZ ->
                    VertexRendering.drawFilledBox(
                        event.worldContext.matrixStack(),
                        consumers.getBuffer(if (chroma ) StellaRenderLayers.CHROMA_3D else StellaRenderLayers.FILLED),
                        x + minX, y + minY, z + minZ,
                        x + maxX, y + maxY, z + maxZ,
                        r,g,b,a
                    )
                }
            }
        }
    }
}