package co.stellarskys.stella.features

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object FeatureLoader {
    private val features = arrayOf(
        "msc.blockOverlay"
    )

    private var moduleCount = 0
    private var moduleErr = 0
    private var loadtime: Long = 0

    fun init() {
        val starttime = System.currentTimeMillis()
        features.forEach { className ->
            try {
                val fullClassName = "meowing.zen.feats.$className"
                Class.forName(fullClassName)
                moduleCount++
            } catch (e: Exception) {
                System.err.println("[Zen] Error initializing $className: $e")
                e.printStackTrace()
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