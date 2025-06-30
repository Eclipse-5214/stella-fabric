package co.stellarskys.stella.features

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.*
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.LocationUtils
import java.lang.reflect.Field

open class Feature(
    private val configName: String? = null,
    private val variable: () -> Boolean = { true },
    area: String? = null,
    subarea: String? = null
) {
    val events = mutableListOf<EventBus.EventCall>()
    private var isRegistered = false
    private val areaLower = area?.lowercase()
    private val subareaLower = subarea?.lowercase()
    private val configValue: () -> Boolean = {
        configName?.let { config.getValue<Boolean>(it) } ?: true
    }


    init {
        initialize()
        configName?.let { Stella.registerListener(it, this) }
        Stella.addFeature(this)
        update()
    }

    open fun initialize() {}

    open fun onRegister() {}

    open fun onUnregister() {}

    fun isEnabled(): Boolean = configValue() && variable()

    fun update() = onToggle(isEnabled() && inArea() && inSubarea())

    @Synchronized
    open fun onToggle(state: Boolean) {
        if (state == isRegistered) return

        if (state) {
            events.forEach { it.register() }
            onRegister()
            isRegistered = true
        } else {
            events.forEach { it.unregister() }
            onUnregister()
            isRegistered = false
        }
    }

    fun inArea(): Boolean = areaLower?.let { LocationUtils.area == it } ?: true

    fun inSubarea(): Boolean = subareaLower?.let { LocationUtils.subarea?.contains(it) == true } ?: true

    inline fun <reified T : Event> register(noinline cb: (T) -> Unit) {
        events.add(EventBus.register<T>(cb, false))
    }
}