package co.stellarskys.stella.utils.render

import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhase
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderPhase
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import net.minecraft.util.Util

object StellaRenderLayers {
    private val CHROMA_STANDARD: MultiPhase = ChromaRenderLayer(
        "stella_standard_chroma",
        RenderLayer.CUTOUT_BUFFER_SIZE,
        false,
        false,
        StellaRenderPipeline.CHROMA_STANDARD(),
        MultiPhaseParameters.builder().build(false)
    )

    private val CHROMA_TEXTURED: java.util.function.Function<Identifier, RenderLayer> = Util.memoize {
            texture -> ChromaRenderLayer(
        "stella_text_chroma",
        RenderLayer.CUTOUT_BUFFER_SIZE,
        false,
        false,
        StellaRenderPipeline.CHROMA_TEXT(),
        MultiPhaseParameters.builder()
            .texture(RenderPhase.Texture(texture, TriState.FALSE, false))
            .build(false)
    )
    }

    fun getChromaStandard() = CHROMA_STANDARD

    fun getChromaTextured(identifier: Identifier) = CHROMA_TEXTURED.apply(identifier)
}