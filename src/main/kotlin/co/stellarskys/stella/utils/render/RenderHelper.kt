package co.stellarskys.stella.utils.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.util.OptionalDouble
import java.util.function.DoubleFunction

object RenderHelper {
    private val linesThroughWallsLayers: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()
    private val linesLayers: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()

    fun renderBlock(context: WorldRenderContext, color: Color, depthCheck: Boolean = true) {
        val hitResult = MinecraftClient.getInstance().crosshairTarget
        if (!(context.blockOutlines() && hitResult is BlockHitResult)) return

        val world = context.world()
        val pos = hitResult.blockPos
        val state = world.getBlockState(pos)

        val matrices = context.matrixStack()!!
        val camera = context.camera().pos

        matrices.push()
        matrices.translate(-camera.x, -camera.y, -camera.z)

        val buffer = context.consumers()!!.getBuffer(
            if (!depthCheck)
                filledThroughWallsLayer else filledLayer
        )

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

        val zFightingOffset = 0.001
        state.getOutlineShape(world, pos).forEachBox { minX, minY, minZ, maxX, maxY, maxZ ->
            VertexRendering.drawFilledBox(
                matrices, buffer,
                (pos.x + minX - zFightingOffset).toFloat(),
                (pos.y + minY - zFightingOffset).toFloat(),
                (pos.z + minZ - zFightingOffset).toFloat(),
                (pos.x + maxX + zFightingOffset).toFloat(),
                (pos.y + maxY + zFightingOffset).toFloat(),
                (pos.z + maxZ + zFightingOffset).toFloat(),
                r, g, b, a
            )
        }

        matrices.pop()
    }

    fun renderOutline(context: WorldRenderContext, color: Color, lineWidth: Float, depthCheck: Boolean = true) {
        val hitResult = MinecraftClient.getInstance().crosshairTarget
        if (!(context.blockOutlines() && hitResult is BlockHitResult)) return

        val world = context.world()
        val pos = hitResult.blockPos
        val state = world.getBlockState(pos)
        val camera = context.camera().pos

        val colorComponents = floatArrayOf(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )

        val zFightingOffset = 0.001
        state.getOutlineShape(world, pos).forEachBox { minX, minY, minZ, maxX, maxY, maxZ ->
            val box = Box(
                pos.x + minX - camera.x - zFightingOffset,
                pos.y + minY - camera.y - zFightingOffset,
                pos.z + minZ - camera.z - zFightingOffset,
                pos.x + maxX - camera.x + zFightingOffset,
                pos.y + maxY - camera.y + zFightingOffset,
                pos.z + maxZ - camera.z + zFightingOffset
            ).offset(camera)

            renderOutlineHelper(
                context, box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                colorComponents, lineWidth, !depthCheck
            )
        }
    }

    fun renderOutlineHelper(
        context: WorldRenderContext,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double,
        color: FloatArray,
        lineWidth: Float,
        throughWalls: Boolean
    ) {
        val matrices = context.matrixStack()!!
        val camera = context.camera().pos

        matrices.push()
        matrices.translate(-camera.x, -camera.y, -camera.z)

        val immediate = context.consumers() as VertexConsumerProvider.Immediate
        val layer = if (throughWalls) getLinesThroughWalls(lineWidth.toDouble()) else getLines(lineWidth.toDouble())
        val buffer = immediate.getBuffer(layer)

        VertexRendering.drawBox(matrices, buffer, minX, minY, minZ, maxX, maxY, maxZ, color[0], color[1], color[2], color[3])
        immediate.draw(layer)

        matrices.pop()
    }

    // --- Custom Render Layers ---

    private val linesThroughWallsPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of("simpleblockoverlay", "pipeline/texture_through_walls_sbo"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    private val filledThroughWallsPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("simpleblockoverlay", "pipeline/debug_filled_box_through_walls"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    private val linesThroughWallsFunc = DoubleFunction { width ->
        RenderLayer.of(
            "lines_through_walls_sbo",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            linesThroughWallsPipeline,
            RenderLayer.MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    private val linesFunc = DoubleFunction { width ->
        RenderLayer.of(
            "lines_sbo",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            RenderPipelines.LINES,
            RenderLayer.MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    fun getLinesThroughWalls(width: Double): RenderLayer.MultiPhase =
        linesThroughWallsLayers.computeIfAbsent(width, linesThroughWallsFunc)

    fun getLines(width: Double): RenderLayer.MultiPhase =
        linesLayers.computeIfAbsent(width, linesFunc)

    val filledLayer: RenderLayer.MultiPhase = RenderLayer.of(
        "filled", RenderLayer.DEFAULT_BUFFER_SIZE, false, true,
        RenderPipelines.DEBUG_FILLED_BOX,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )

    val filledThroughWallsLayer: RenderLayer.MultiPhase = RenderLayer.of(
        "filled_through_walls", RenderLayer.DEFAULT_BUFFER_SIZE, false, true,
        filledThroughWallsPipeline,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )
}
