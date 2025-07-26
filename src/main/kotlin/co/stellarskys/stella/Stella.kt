package co.stellarskys.stella

import co.stellarskys.novaconfig.utils.chatutils
import co.stellarskys.stella.events.*
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.FeatureManager
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.LocalStore
import co.stellarskys.stella.utils.LocalStores
import co.stellarskys.stella.utils.ScoreboardUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import com.mojang.brigadier.Command
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.util.Identifier
import java.util.concurrent.ConcurrentHashMap

class Stella : ClientModInitializer {
	private var shown = false

	@Target(AnnotationTarget.CLASS)
	annotation class Command

	override fun onInitializeClient() {
		init()
		FeatureManager.init()
		LocalStores.init()

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
		EventBus.register<GuiEvent.Open> ({ event ->
			if (event.screen is InventoryScreen) isInInventory = true
		})

		EventBus.register<GuiEvent.Close> ({
			isInInventory = false
		})

		EventBus.register<TickEvent.Client>({
			ScoreboardUtils.sidebarLines = ScoreboardUtils.fetchScoreboardLines().map { l -> ScoreboardUtils.cleanSB(l) }
		})

		EventBus.register<AreaEvent.Main> ({ updateFeatures() })
		EventBus.register<AreaEvent.Sub> ({ updateFeatures() })

		/*
		val test = LocalStore("general","./config/stella/test.json")
		test["hi"] = false
		 */

		val room = DungeonScanner.currentRoom
		val floor = Dungeon.floor
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
		val PREFIX: String = "§d[Stella]"
		val SHORTPREFIX: String = "§d[SA]"

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

		fun getResource(path: String) = Identifier.of(NAMESPACE, path)

		fun init() {}
	}
}

