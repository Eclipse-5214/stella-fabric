package co.stellarskys.stella.utils


import net.minecraft.client.MinecraftClient


object GuiScreenUtils {

    private val mc get() = MinecraftClient.getInstance()

    val scaledWindowHeight: Int
        get() = mc.window.scaledHeight.toInt()

    val scaledWindowWidth: Int
        get() = mc.window.scaledWidth.toInt()

    val displayWidth: Int
        get() = mc.window.width.toInt()

    val displayHeight: Int
        get() = mc.window.height.toInt()

    val scaleFactor: Int
        get() = mc.window.scaleFactor.toInt()

    private val globalMouseX get() = MouseCompat.getX()
    private val globalMouseY get() = MouseCompat.getY()

    val mouseX: Int get() {
        var x = (globalMouseX * scaledWindowWidth / displayWidth).toInt()
        if (mc.window.framebufferWidth > mc.window.width) x *= 2
        return x
    }

    val mouseY: Int
        get() {
            val height = this.scaledWindowHeight
            var y = (globalMouseY * height / displayHeight).toInt()
            if (mc.window.framebufferHeight > mc.window.height) y *= 2
            return y
        }

    val mousePos: Pair<Int, Int> get() = mouseX to mouseY
}