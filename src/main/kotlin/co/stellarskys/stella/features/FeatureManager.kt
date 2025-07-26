package co.stellarskys.stella.features

import co.stellarskys.stella.Stella
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.reflections.Reflections

object FeatureManager {
    private val features = arrayOf(
        "msc.blockOverlay"
    )

    private var moduleCount = 0
    private var moduleErr = 0
    private var loadtime: Long = 0

    fun init() {
        val reflections = Reflections("co.stellarskys")

        val starttime = System.currentTimeMillis()
        features.forEach { className ->
            try {
                val fullClassName = "co.stellarskys.stella.features.$className"
                Class.forName(fullClassName)
                moduleCount++
            } catch (e: Exception) {
                System.err.println("[Stella] Error initializing $className: $e")
                e.printStackTrace()
            }
        }

        val commands = reflections.getTypesAnnotatedWith(Stella.Command::class.java)
        val commandCount = commands.size

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

        //CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            //calculator.register(dispatcher)
        //}
        //ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            //carrycommand.register(dispatcher)
            //slayerstatscommand.register(dispatcher)
        //}
        //carryhud.initialize()
        loadtime = System.currentTimeMillis() - starttime
    }

    fun getFeatCount(): Int = moduleCount
    fun getLoadtime(): Long = loadtime
}