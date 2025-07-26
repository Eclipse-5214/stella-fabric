package co.stellarskys.stella.hud

import net.minecraft.client.gui.DrawContext

object HUDManager {
    private val elements = mutableMapOf<String, String>()
    private val customRenderers = mutableMapOf<String, (DrawContext, Float, Float, Int, Int, Float, Float, Boolean) -> Unit>()
    private val customDimensions = mutableMapOf<String, Pair<Int, Int>>()
    private val dataStore = HUDDataStore("hud_positions")

    fun register(name: String, exampleText: String) {
        elements[name] = exampleText
    }

    fun registerCustom(name: String, width: Int, height: Int, renderer: (DrawContext, Float, Float, Int, Int, Float, Float, Boolean) -> Unit) {
        elements[name] = ""
        customRenderers[name] = renderer
        customDimensions[name] = width to height
    }

    private val positions get() = dataStore.getPositions().positions

    fun getElements(): Map<String, String> = elements

    fun getCustomRenderer(name: String): ((DrawContext, Float, Float, Int, Int, Float, Float, Boolean) -> Unit)? = customRenderers[name]

    fun getCustomDimensions(name: String): Pair<Int, Int>? = customDimensions[name]

    fun getX(name: String): Float = positions[name]?.x ?: 50f
    fun getY(name: String): Float = positions[name]?.y ?: 50f
    fun getScale(name: String): Float = positions[name]?.scale ?: 1f
    fun isEnabled(name: String): Boolean = positions[name]?.enabled ?: true

    fun setPosition(name: String, x: Float, y: Float, scale: Float = 1f, enabled: Boolean = true) {
        positions[name] = HUDPosition(x, y, scale, enabled)
        dataStore.savePositions(HUDPositions(positions))
    }

    fun toggle(name: String) {
        val current = positions[name] ?: HUDPosition(10f, 10f)
        positions[name] = current.copy(enabled = !current.enabled)
        dataStore.savePositions(HUDPositions(positions))
    }
}
