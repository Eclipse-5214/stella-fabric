package co.stellarskys.stella.features

import co.stellarskys.novaconfig.utils.chatutils
import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.*
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.LocationUtils
import java.lang.reflect.Field

open class Feature(
    private val configName: String? = null,
    area: Any? = null,
    subarea: Any? = null
) {
    val events = mutableListOf<EventBus.EventCall>()
    private var isRegistered = false

    private val areas = when (area) {
        is String -> listOf(area.lowercase())
        is List<*> -> area.filterIsInstance<String>().map { it.lowercase() }
        else -> emptyList()
    }

    private val subareas = when (subarea) {
        is String -> listOf(subarea.lowercase())
        is List<*> -> subarea.filterIsInstance<String>().map { it.lowercase() }
        else -> emptyList()
    }

    private val configValue: () -> Boolean = {
        configName?.let { config.getValue<Boolean>(it) } ?: true
    }

    init {
        initialize()
        configName?.let { Stella.registerListener(it, this) }
        Stella.addFeature(this)
        update()
    }

    protected val mc = Stella.mc
    protected val fontRenderer = mc.textRenderer
    protected inline val config get() = co.stellarskys.stella.utils.config
    protected inline val player get() = mc.player
    protected inline val world get() = mc.world
    protected inline val window get() = mc.window
    protected inline val mouseX get() = mc.mouse.x * window.scaledWidth / window.width
    protected inline val mouseY get() = mc.mouse.y * window.scaledWidth / window.width

    open fun initialize() {}

    open fun onRegister() {}

    open fun onUnregister() {}

    fun isEnabled(): Boolean = configValue() && inArea() && inSubarea()

    fun update() = onToggle(isEnabled())

    @Synchronized
    open fun onToggle(state: Boolean) {
        ChatUtils.addMessage("$configName feture update called!")
        ChatUtils.addMessage("feture state is $state!")
        if (state == isRegistered) return

        if (state) {
            ChatUtils.addMessage("registering feature")
            events.forEach { it.register() }
            onRegister()
            isRegistered = true
        } else {
            ChatUtils.addMessage("unrgistering feature")
            events.forEach { it.unregister() }
            onUnregister()
            isRegistered = false
        }
    }

    fun inArea(): Boolean = areas.isEmpty() || areas.any { LocationUtils.checkArea(it) }

    fun inSubarea(): Boolean = subareas.isEmpty() || subareas.any { LocationUtils.checkSubarea(it) }

    inline fun <reified T : Event> register(noinline cb: (T) -> Unit) {
        events.add(EventBus.register<T>(cb, false))
    }

    fun hasAreas(): Boolean = areas.isNotEmpty()
    fun hasSubareas(): Boolean = subareas.isNotEmpty()
}