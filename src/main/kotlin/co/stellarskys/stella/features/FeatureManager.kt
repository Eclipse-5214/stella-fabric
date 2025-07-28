package co.stellarskys.stella.features

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.TimeUtils.millis
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.reflections.Reflections

object FeatureManager {
    private val features = arrayOf(
        "msc.blockOverlay",
        "dungeons.roomName"
    )

    private var moduleCount = 0
    private var commandCount = 0
    private var loadtime: Long = 0

    fun init() {
        val reflections = Reflections("co.stellarskys")

        val features = reflections.getTypesAnnotatedWith(Stella.Module::class.java)
        val starttime = TimeUtils.now
        val categoryOrder = listOf("dungeons", "stellanav", "msc")

        features.sortedWith(compareBy<Class<*>> { clazz ->
            val packageName = clazz.`package`.name
            val category = packageName.substringAfterLast(".")
            categoryOrder.indexOf(category).takeIf { it != -1 } ?: Int.MAX_VALUE
        }.thenBy { it.name }).forEach { clazz ->
            try {
                Class.forName(clazz.name)
                moduleCount++
            } catch (e: Exception) {
                System.err.println("[Zen] Error initializing ${clazz.name}: $e")
                e.printStackTrace()
            }
        }

        val commands = reflections.getTypesAnnotatedWith(Stella.Command::class.java)
        commandCount = commands.size

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { commandClass ->
                try {
                    val commandInstance = commandClass.getDeclaredField("INSTANCE").get(null)
                    val registerMethod = commandClass.methods.find { it.name == "register" } // a bit eh but it works
                    registerMethod?.invoke(commandInstance, dispatcher)
                } catch (e: Exception) {
                    System.err.println("[Stella] Error initializing ${commandClass.name}: $e")
                    e.printStackTrace()
                }
            }
        }

        loadtime = starttime.since.millis
    }

    fun getFeatCount(): Int = moduleCount
    fun getCommandCount(): Int = commandCount
    fun getLoadtime(): Long = loadtime
}