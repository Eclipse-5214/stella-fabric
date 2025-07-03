package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.render.StellaRenderPipelineUtils.commonChromaUniforms
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

enum class StellaRenderPipeline(
    snippet: RenderPipeline.Snippet,
    vFormat: VertexFormat = VertexFormats.POSITION_COLOR,
    vDrawMode: VertexFormat.DrawMode = VertexFormat.DrawMode.QUADS,
    blend: BlendFunction? = null,
    withCull: Boolean? = false,
    vertexShaderPath: String? = null,
    fragmentShaderPath: String? = vertexShaderPath,
    sampler: String? = null,
    uniforms: Map<String, UniformType> = emptyMap(),
    depthWrite: Boolean = true,
    depthTestFunction: DepthTestFunction = DepthTestFunction.LEQUAL_DEPTH_TEST,
) {

    CHROMA_STANDARD(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "standard_chroma",
        uniforms = commonChromaUniforms,
    ),
    CHROMA_TEXT(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_TEXTURE_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "textured_chroma",
        sampler = "Sampler0",
        uniforms = commonChromaUniforms,
    ),
    ;

    private val _pipe: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(snippet)
            .withLocation(Identifier.of(Stella.NAMESPACE, this.name.lowercase()))
            .withVertexFormat(vFormat, vDrawMode)
            .apply {
                // One or the other, never both
                blend?.let(this::withBlend) ?: withCull?.let(this::withCull)
                vertexShaderPath?.let { withVertexShader(Identifier.of(Stella.NAMESPACE, it)) }
                fragmentShaderPath?.let { withFragmentShader(Identifier.of(Stella.NAMESPACE, it)) }
                sampler?.let(this::withSampler)
                uniforms.forEach(this::withUniform)
                withDepthWrite(depthWrite)
                withDepthTestFunction(depthTestFunction)
            }.build(),
    )

    operator fun invoke(): RenderPipeline = _pipe
}

private object StellaRenderPipelineUtils {
    val commonChromaUniforms = mapOf(
        "chromaSize" to UniformType.FLOAT,
        "timeOffset" to UniformType.FLOAT,
        "saturation" to UniformType.FLOAT,
        "forwardDirection" to UniformType.INT,
    )
}