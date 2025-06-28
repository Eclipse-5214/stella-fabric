package co.stellarskys.stella

import co.stellarskys.stella.features.msc.blockOverlay
import co.stellarskys.stella.utils.config
import com.mojang.brigadier.Command
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer

object Stella : ClientModInitializer {
	const val NAMESPACE: String = "stella"
	val STELLA_MOD: ModContainer = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow()
	val VERSION: String? = STELLA_MOD.getMetadata().getVersion().getFriendlyString()
	val INSTANCE: Stella? = null

	override fun onInitializeClient() {
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

		WorldRenderEvents.LAST.register{ context -> blockOverlay.renderOverlayBox(context) }
		WorldRenderEvents.LAST.register{ context -> blockOverlay.renderOutline(context) }

	}
}