package co.stellarskys.stella.features.msc

import co.stellarskys.novaconfig.RGBA
import co.stellarskys.stella.utils.config
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhase
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.RenderPhase.LineWidth
import net.minecraft.client.render.VertexConsumerProvider.Immediate
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.VertexRendering
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.shape.VoxelShapes.BoxConsumer
import java.awt.Color
import java.util.*
import java.util.function.DoubleFunction

object blockOverlay {
    private val LINES_THROUGH_WALLS_LAYERS_SBO: Double2ObjectMap<MultiPhase?> = Double2ObjectOpenHashMap<MultiPhase?>()
    private val LINES_LAYERS_SBO: Double2ObjectMap<MultiPhase?> = Double2ObjectOpenHashMap<MultiPhase?>()

    fun renderOverlayBox(context: WorldRenderContext) {
        if (!(config["overlayEnabled"] as Boolean) || !(config["fillBlockOverlay"] as Boolean)) return

        val hitResult = MinecraftClient.getInstance().crosshairTarget
        if (!(context.blockOutlines() && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)) {
            return
        }

        val world = context.world()
        val pos = (hitResult as BlockHitResult).getBlockPos()
        val state = world.getBlockState(pos)

        val matrices = context.matrixStack()
        val camera = context.camera().getPos()

        matrices!!.push()
        matrices.translate(-camera.x, -camera.y, -camera.z)

        val consumers = context.consumers()
        val buffer =
            consumers!!.getBuffer(FILLED_SBO)

        val color: Color = (config["blockFillColor"] as RGBA).toColor(true)

        state.getOutlineShape(world, pos)
            .forEachBox(BoxConsumer { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
                // We make the cube a tiny bit larger to avoid z fighting
                val zFightingOffset = 0.001
                VertexRendering.drawFilledBox(
                    matrices,
                    buffer,
                    (pos.getX() + minX - zFightingOffset).toFloat(),
                    (pos.getY() + minY - zFightingOffset).toFloat(),
                    (pos.getZ() + minZ - zFightingOffset).toFloat(),
                    (pos.getX() + maxX + zFightingOffset).toFloat(),
                    (pos.getY() + maxY + zFightingOffset).toFloat(),
                    (pos.getZ() + maxZ + zFightingOffset).toFloat(),
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    color.getAlpha() / 255f
                )
            })

        matrices.pop()
    }

    fun renderOutline(context: WorldRenderContext) {
        if (!(config["overlayEnabled"] as Boolean)) return
        //if (SimpleBlockOverlayConfig.CONFIG.instance().disableOutline) return

        val hitResult = MinecraftClient.getInstance().crosshairTarget
        if (!(context.blockOutlines() && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)) {
            return
        }

        val world = context.world()
        val pos = (hitResult as BlockHitResult).getBlockPos()
        val state = world.getBlockState(pos)

        val color: Color = (config["blockHighlightColor"] as RGBA).toColor(true)
        val colorComponents = floatArrayOf(
            color.getRed() / 255f,
            color.getGreen() / 255f,
            color.getBlue() / 255f,
            color.getAlpha() / 255f
        )

        val cameraX = context.camera().getPos().getX().toFloat()
        val cameraY = context.camera().getPos().getY().toFloat()
        val cameraZ = context.camera().getPos().getZ().toFloat()

        state.getOutlineShape(world, pos)
            .forEachBox(BoxConsumer { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
                // We make the cube a tiny bit larger to avoid z fighting
                val zFightingOffset = 0.001
                val box = Box(
                    (pos.getX() + minX - cameraX - zFightingOffset).toFloat().toDouble(),
                    (pos.getY() + minY - cameraY - zFightingOffset).toFloat().toDouble(),
                    (pos.getZ() + minZ - cameraZ - zFightingOffset).toFloat().toDouble(),
                    (pos.getX() + maxX - cameraX + zFightingOffset).toFloat().toDouble(),
                    (pos.getY() + maxY - cameraY + zFightingOffset).toFloat().toDouble(),
                    (pos.getZ() + maxZ - cameraZ + zFightingOffset).toFloat().toDouble()
                ).offset(context.camera().getPos())
                renderOutlineHelper(
                    context,
                    box.minX,
                    box.minY,
                    box.minZ,
                    box.maxX,
                    box.maxY,
                    box.maxZ,
                    colorComponents,
                    (config["overlayLineWidth"] as Int).toFloat(),
                    false
                )
            })
    }

    /**
     * This method is taken from Skyblocker under the LGPL 3.0 license
     * [license](https://github.com/SkyblockerMod/Skyblocker/blob/master/LICENSE)
     * It has been updated to support multiversion
     * [class](https://github.com/SkyblockerMod/Skyblocker/blob/master/src/main/java/de/hysky/skyblocker/utils/render/RenderHelper.java)
     */
    fun renderOutlineHelper(
        context: WorldRenderContext,
        minX: Double,
        minY: Double,
        minZ: Double,
        maxX: Double,
        maxY: Double,
        maxZ: Double,
        colorComponents: FloatArray,
        lineWidth: Float,
        throughWalls: Boolean
    ) {
        val matrices = context.matrixStack()
        val camera = context.camera().getPos()

        matrices!!.push()
        matrices.translate(-camera.getX(), -camera.getY(), -camera.getZ())

        val consumers = context.consumers() as Immediate?
        val layer: RenderLayer? =
            if (throughWalls) getLinesThroughWalls(lineWidth.toDouble()) else getLines(lineWidth.toDouble())
        val buffer = consumers!!.getBuffer(layer)

        VertexRendering.drawBox(
            matrices,
            buffer,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            colorComponents[0],
            colorComponents[1],
            colorComponents[2],
            colorComponents[3]
        )
        consumers.draw(layer)

        matrices.pop()
    }

    /**
     * These are also taken from Skyblocker under the LGPL 3.0 license
     * [license](https://github.com/SkyblockerMod/Skyblocker/blob/master/LICENSE)
     * I currently don't know how these work so I sort of just copied them for my own use here for now.
     * [class](https://github.com/SkyblockerMod/Skyblocker/blob/master/src/main/java/de/hysky/skyblocker/utils/render/RenderHelper.java)
     */
    val LINES_THROUGH_WALLS_S: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of("simpleblockoverlay", "pipeline/texture_through_walls_sbo"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val FILLED_THROUGH_WALLS_S: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("simpleblockoverlay", "pipeline/debug_filled_box_through_walls"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    private val LINES_THROUGH_WALLS_SBO = DoubleFunction { lineWidth: Double ->
        RenderLayer.of(
            "lines_through_walls_sbo",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            false,
            false,
            LINES_THROUGH_WALLS_S,
            RenderLayer.MultiPhaseParameters.builder()
                .lineWidth(LineWidth(OptionalDouble.of(lineWidth)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    private val LINES_SBO = DoubleFunction { lineWidth: Double ->
        RenderLayer.of(
            "lines_sbo",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            false,
            false,
            RenderPipelines.LINES,
            RenderLayer.MultiPhaseParameters.builder()
                .lineWidth(LineWidth(OptionalDouble.of(lineWidth)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    fun getLinesThroughWalls(lineWidth: Double): MultiPhase? {
        return LINES_THROUGH_WALLS_LAYERS_SBO.computeIfAbsent(lineWidth, LINES_THROUGH_WALLS_SBO)
    }

    fun getLines(lineWidth: Double): MultiPhase? {
        return LINES_LAYERS_SBO.computeIfAbsent(lineWidth, LINES_SBO)
    }

    val FILLED_SBO: MultiPhase = RenderLayer.of(
        "filled",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        RenderPipelines.DEBUG_FILLED_BOX,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )

    val FILLED_THROUGH_WALLS_SBO: MultiPhase = RenderLayer.of(
        "filled_through_walls",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        FILLED_THROUGH_WALLS_S,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )
}

object OverlayToggle {
    @JvmStatic
    fun shouldDisableVanillaOutline(): Boolean {
        return config["overlayEnabled"] as Boolean
    }
}
