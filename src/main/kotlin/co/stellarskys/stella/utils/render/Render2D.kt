package co.stellarskys.stella.utils.render


import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.utils.clearCodes
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import java.awt.Color
import kotlin.math.max

object Render2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()
    val textRenderer = mc.textRenderer
    val window get() = mc.window
    val mouse = mc.mouse
    val screenWidth get() = window.width
    val screenHeight get() = window.height
    val scaledWidth get() = window.scaledWidth
    val scaledHeight get() = window.scaledHeight

    @JvmOverloads
    fun drawString(ctx: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.matrices
        if (scale != 1f) {
            matrices.push()
            matrices.scale(scale, scale, 1f)
        }

        ctx.drawText(
            textRenderer,
            str.replace(formattingRegex, "${Formatting.FORMATTING_CODE_PREFIX}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.pop()
    }

    @JvmOverloads
    fun drawStringNW(ctx: DrawContext, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        var yy = y
        str.split("\n").forEach {
            drawString(ctx, it, x, yy, scale, shadow)
            yy += 10
        }
    }

    @JvmOverloads
    fun drawRect(ctx: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderLayer.getGui(), x, y, x + width, y + height, color.rgb)
    }

    fun String.width(): Int {
        val newlines = this.split("\n")
        if (newlines.size <= 1) return textRenderer.getWidth(this.clearCodes())

        var maxWidth = 0

        for (line in newlines)
            maxWidth = max(maxWidth, textRenderer.getWidth(line.clearCodes()))

        return maxWidth
    }

    fun String.height(): Int {
        val newlines = this.split("\n")
        if (newlines.size <= 1) return textRenderer.fontHeight

        return textRenderer.fontHeight * (newlines.size + 1)
    }

    object Mouse {
        val x get() = mouse.x * scaledWidth / max(1, screenWidth)
        val y get() = mouse.y * scaledHeight / max(1, screenHeight)
    }

    fun renderString(context: DrawContext, text: String, x: Float, y: Float, scale: Float) {
        context.matrices.push()
        context.matrices.translate(x.toDouble(), y.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(mc.textRenderer, text, 0, 0, Colors.WHITE, false)
        context.matrices.pop()
    }

    fun renderStringWithShadow(context: DrawContext, text: String, x: Float, y: Float, scale: Float) {
        context.matrices.push()
        context.matrices.translate(x.toDouble(), y.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(mc.textRenderer, text, 0, 0, Colors.WHITE, true)
        context.matrices.pop()
    }

    fun renderItem(context: DrawContext, item: ItemStack, x: Float, y: Float, scale: Float) {
        val matrixStack = context.matrices
        matrixStack.push()
        matrixStack.translate(x, y, 0.0f)
        matrixStack.scale(scale, scale, 1f)
        context.drawItem(item, 0, 0)
        matrixStack.pop()
    }
}