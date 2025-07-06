package co.stellarskys.stella.utils.render

import co.stellarskys.novaconfig.RGBA
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object RenderHelper {
    fun renderFilled(context: WorldRenderContext, pos: BlockPos, colorComponents: RGBA, throughWalls: Boolean, chroma: Boolean) {
        renderFilled(context, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x.toDouble() + 1, pos.y.toDouble() + 1, pos.z.toDouble() + 1, colorComponents, throughWalls, chroma)
    }

    fun renderFilled(context: WorldRenderContext, pos: Vec3d, dimensions: Vec3d, colorComponents: RGBA, throughWalls: Boolean, chroma: Boolean) {
        renderFilled(context, pos.x, pos.y, pos.z, pos.x + dimensions.x, pos.y + dimensions.y, pos.z + dimensions.z, colorComponents, throughWalls, chroma)
    }

    fun renderFilled(context: WorldRenderContext, box: Box, colorComponents: RGBA, throughWalls: Boolean, chroma: Boolean) {
        renderFilled(context, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, colorComponents, throughWalls, chroma)
    }

    fun renderFilled(context: WorldRenderContext, minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, colorComponents: RGBA, throughWalls: Boolean, chroma: Boolean){
        renderFilledInternal(context, minX, minY, minZ, maxX, maxY, maxZ, colorComponents, throughWalls, chroma);
    }

    private fun renderFilledInternal(context: WorldRenderContext, minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, colorComponents: RGBA, throughWalls: Boolean, chroma: Boolean){
        val (rRaw, gRaw, bRaw, aRaw) = colorComponents
        val r = rRaw / 255f
        val g = gRaw / 255f
        val b = bRaw / 255f
        val a = aRaw / 255f

        println("[RenderHelper] Filling Box: ($minX,$minY,$minZ) → ($maxX,$maxY,$maxZ)")
        println("[RenderHelper] RGBA: ($rRaw, $gRaw, $bRaw, $aRaw) → normalized = ($r, $g, $b, $a)")
        println("[RenderHelper] Layer: ${when {
            chroma -> "CHROMA_3D"
            throughWalls -> "FILLEDTHROUGHWALLS"
            else -> "FILLED"
        }}")

        val matrices: MatrixStack = context.matrixStack()?: return
        val camera: Vec3d = context.camera().pos

        println("[RenderHelper] Camera Position: (${camera.x}, ${camera.y}, ${camera.z})")

        matrices.push()
        matrices.translate(-camera.x, -camera.y, -camera.z)

        val consumers: VertexConsumerProvider = context.consumers()?: return
        val layer = when {
            chroma -> StellaRenderLayers.CHROMA_3D
            throughWalls -> StellaRenderLayers.FILLEDTHROUGHWALLS
            else -> StellaRenderLayers.FILLED
        }

        println("[RenderHelper] Got past consumers")

        val buffer: VertexConsumer = consumers.getBuffer(layer)

        VertexRendering.drawFilledBox(matrices, buffer, minX, minY, minZ, maxX, maxY, maxZ,1f,1f,1f,1f)

        matrices.pop()
    }

    fun renderOutline(context: WorldRenderContext, pos: BlockPos, colorComponents: RGBA, lineWidth: Float, throughWalls: Boolean, chroma: Boolean) {
        renderOutline(context, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 1.0, colorComponents, lineWidth, throughWalls, chroma)
    }

    fun renderOutline(context: WorldRenderContext, pos: Vec3d, dimensions: Vec3d, colorComponents: RGBA, lineWidth: Float, throughWalls: Boolean, chroma: Boolean) {
        renderOutline(context, pos.x, pos.y, pos.z, pos.x + dimensions.x, pos.y + dimensions.y, pos.z + dimensions.z, colorComponents, lineWidth, throughWalls, chroma)
    }

    fun renderOutline(context: WorldRenderContext, box: Box, colorComponents: RGBA, lineWidth: Float,throughWalls: Boolean, chroma: Boolean) {
        renderOutline(context, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, colorComponents, lineWidth, throughWalls, chroma)
    }

    fun renderOutline(context: WorldRenderContext, minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, colorComponents: RGBA, lineWidth: Float, throughWalls: Boolean, chroma: Boolean) {
            val (rRaw, gRaw, bRaw, aRaw) = colorComponents
            val r = rRaw / 255f
            val g = gRaw / 255f
            val b = bRaw / 255f
            val a = aRaw / 255f

        println("[RenderHelper] Drawing Outline: ($minX,$minY,$minZ) → ($maxX,$maxY,$maxZ)")
        println("[RenderHelper] RGBA: ($rRaw, $gRaw, $bRaw, $aRaw) → normalized = ($r, $g, $b, $a), lineWidth = $lineWidth")
        println("[RenderHelper] Layer: ${when {
            chroma -> "CHROMA_LINES"
            throughWalls -> "LINES_THROUGH_WALLS"
            else -> "LINES"
        }}")

            val matrices: MatrixStack = context.matrixStack()?: return
            val camera: Vec3d = context.camera().pos

        println("[RenderHelper] Camera Position: (${camera.x}, ${camera.y}, ${camera.z})")

            matrices.push()
            matrices.translate(-camera.x, -camera.y, -camera.z)

            val consumers: VertexConsumerProvider.Immediate = context.consumers() as? VertexConsumerProvider.Immediate?: return
            val layer = when {
                chroma -> StellaRenderLayers.getChromaLines(lineWidth.toDouble())
                throughWalls -> StellaRenderLayers.getLinesThroughWalls(lineWidth.toDouble())
                else -> StellaRenderLayers.getLines(lineWidth.toDouble())
            }

        println("[RenderHelper] Got past consumers")

            val buffer: VertexConsumer = consumers.getBuffer(layer)

            VertexRendering.drawBox(matrices,buffer,minX,minY,minZ,maxX,maxY,maxZ,r,g,b,a)
            consumers.draw(layer)

            matrices.pop()
    }
}
