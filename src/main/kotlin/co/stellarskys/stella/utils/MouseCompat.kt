package co.stellarskys.stella.utils

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwGetMouseButton
import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import net.minecraft.client.MinecraftClient

object MouseCompat {
    private val window: Long
        get() = MinecraftClient.getInstance().window.handle

    fun isButtonDown(button: Int): Boolean =
        glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS

    fun getX(): Double {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        glfwGetCursorPos(window, x, y)
        return x[0]
    }

    fun getY(): Double {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        glfwGetCursorPos(window, x, y)
        return y[0]
    }

    // Scroll and event-based methods would need to be handled via GLFW callbacks
}
