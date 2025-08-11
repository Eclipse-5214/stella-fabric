package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.GuiEvent
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.*
import co.stellarskys.stella.features.stellanav.utils.render.mapRender
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.CommandUtils
import co.stellarskys.stella.utils.config.core.Config
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.height
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.*
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import java.awt.Color

@Stella.Module
object map: Feature("mapEnabled", area = "catacombs") {
    private const val name = "StellaNav"

    override fun initialize() {
        HUDManager.registerCustom(name, 148, 148, this::HUDEditorRender)

        register<GuiEvent.HUD> { event ->
            if (HUDManager.isEnabled(name)) RenderMap(event.context)
        }
    }

    fun HUDEditorRender(context: DrawContext, x: Float, y: Float, width: Int, height: Int, scale: Float, partialTicks: Float, previewMode: Boolean){
        mapRender.renderPreview(context, x, y, scale)
    }

    fun RenderMap(context: DrawContext) {
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)

        mapRender.render(context, x, y, scale)
    }
}