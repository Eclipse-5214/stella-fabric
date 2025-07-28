package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.Stella.Companion.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Identifier

object Render2D {
    fun renderString(context: DrawContext, text: String, x: Float, y: Float, scale: Float) {
        context.matrices.push()
        context.matrices.translate(x.toDouble(), y.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(mc.textRenderer, text, 0, 0, Colors.WHITE, false)
        context.matrices.pop()
    }

    fun renderText(context: DrawContext, text: Text, x: Float, y: Float, scale: Float){
        context.matrices.push()
        context.matrices.translate(x.toDouble(), y.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(mc.textRenderer, text, 0, 0, Colors.WHITE, false)
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