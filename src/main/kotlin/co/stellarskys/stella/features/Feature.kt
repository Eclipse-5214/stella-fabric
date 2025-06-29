package co.stellarskys.stella.features

import co.stellarskys.stella.events.*
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.LocationUtils

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
    private var configVal: Boolean? = null

    init {
        configVal == config.getConfigValue<Boolean>(configName as String)
        initialize()

        config.config.registerListener { cfgName, value ->
            if (configName == cfgName){
                configVal = value as Boolean
            }
        }
        update()
    }

    open fun initialize() {}

    open fun onRegister() {}

    open fun onUnregister() {}

    fun isEnabled(): Boolean {
        return try {
            val configEnabled = configField?.get(Zen.config) as? Boolean ?: true
            configEnabled && variable()
        } catch (e: Exception) {
            variable()
        }
    }

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