package co.stellarskys.stella

import co.stellarskys.stella.events.*
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.FeatureManager
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.config
import com.mojang.brigadier.Command
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.MinecraftClient
import java.util.concurrent.ConcurrentHashMap

class Stella : ClientModInitializer {
	private var shown = false

	override fun onInitializeClient() {
		init()
		FeatureManager.init()

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			val cmd = Command<FabricClientCommandSource> { context ->
				config.open()
				println("command executed")
				1
			}

			dispatcher.register(
				ClientCommandManager.
				literal("stella").
				executes(cmd)
			)
		}

		ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			if (shown) return@register
			ChatUtils.addMessage(
				"§d[Stella] §nMod loaded - §c${FeatureManager.getFeatCount()} §bfeatures",
				"§d${FeatureManager.getLoadtime()}ms §7| §d7 utils §7| §d1 command"
			)
			//UpdateChecker.checkForUpdates()
			shown = true
		}

		// Setup Listeners
		config.registerListener{ name, value ->
			configListeners[name]?.forEach { it.update() }
			ConfigCallback[name]?.forEach { it() }
		}

		// Event stuff
		EventBus.register<AreaEvent> ({ updateFeatures() })
		EventBus.register<SubAreaEvent> ({ updateFeatures() })

	}

	companion object {
		private val features = mutableListOf<Feature>()
		private val configListeners = ConcurrentHashMap<String, MutableList<Feature>>()
		private val ConfigCallback = ConcurrentHashMap<String, MutableList<() -> Unit>>()
		val mc = MinecraftClient.getInstance()
		val NAMESPACE: String = "stella"
		val STELLA_MOD: ModContainer = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow()
		val VERSION: String? = STELLA_MOD.getMetadata().getVersion().getFriendlyString()
		val INSTANCE: Stella? = null

		var isInInventory = false


		fun addFeature(feature: Feature) {
			features.add(feature)
		}

		fun registerListener(configName: String, feature: Feature) {
			configListeners.getOrPut(configName) { mutableListOf() }.add(feature)
		}

		fun registerListener(configName: String, callback: () -> Unit) {
			ConfigCallback.getOrPut(configName) { mutableListOf() }.add(callback)
		}

		fun updateFeatures() {
			features.forEach { it.update() }
		}

		fun init() {}
	}
}

