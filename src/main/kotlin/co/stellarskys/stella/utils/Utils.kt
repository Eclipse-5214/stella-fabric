package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.universal.UMatrixStack
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.particle.SimpleParticleType
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.apache.commons.lang3.SystemUtils

object Utils {
    private val emoteRegex = "[^\\u0000-\\u007F]".toRegex()

    fun playSound(sound: SoundEvent, volume: Float, pitch: Float) {
        MinecraftClient.getInstance().player?.playSound(sound, volume, pitch)
    }

    fun spawnParticle(particle: SimpleParticleType?, x: Double, y: Double, z: Double) {
        spawnParticle(particle, x, y, z, 0.0, 0.0, 0.0)
    }

    fun spawnParticle(particle: SimpleParticleType?, x: Double, y: Double, z: Double, velocityX: Double, velocityY: Double, velocityZ: Double) {
        val mc = MinecraftClient.getInstance()
        mc.world?.addParticleClient(ParticleTypes.FLAME, x, y, z, velocityX, velocityY, velocityZ)
    }

    fun spawnParticleAtPlayer(particle: SimpleParticleType?, velocityX: Double, velocityY: Double, velocityZ: Double) {
        val mc = MinecraftClient.getInstance()
        mc.player?.let { player ->
            spawnParticle(
                particle,
                player.x,
                player.y + 1.0,
                player.z,
                velocityX, velocityY, velocityZ
            )
        }
    }

    fun showTitle(title: String?, subtitle: String?, duration: Int) {
        val mc = MinecraftClient.getInstance()
        mc.inGameHud.setTitle(title?.let { Text.literal(it) })
        mc.inGameHud.setSubtitle(subtitle?.let { Text.literal(it) })
    }

    fun showTitle(title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
        val mc = MinecraftClient.getInstance()
        mc.inGameHud.setTitleTicks(fadeIn, stay, fadeOut)
        mc.inGameHud.setTitle(Text.literal(title))
        mc.inGameHud.setSubtitle(Text.literal(subtitle))
    }

    fun String.removeFormatting(): String {
        return this.replace(Regex("[§&][0-9a-fk-or]", RegexOption.IGNORE_CASE), "")
    }

    fun String.removeEmotes() = replace(emoteRegex, "")

    fun getPartialTicks(): Float = MinecraftClient.getInstance().renderTickCounter.getTickProgress(true)

    fun FloatArray.toColorInt(hasAlpha: Boolean = size > 3): Int {
        val r = (this[0] * 255f + 0.5f).toInt() and 0xFF
        val g = (this[1] * 255f + 0.5f).toInt() and 0xFF
        val b = (this[2] * 255f + 0.5f).toInt() and 0xFF
        val a = if (hasAlpha) (this[3] * 255f + 0.5f).toInt() and 0xFF else 0xFF

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun createBlock(radius: Float = 0f): UIComponent {
        return if (SystemUtils.IS_OS_MAC_OSX) UIBlock() else UIRoundedRectangle(radius)
    }

    inline fun <reified R> Any.getField(name: String): R = javaClass.getDeclaredField(name).apply { isAccessible = true }[this] as R
}

object WorldUtils {
    fun getBlockStateAt(x: Int, y: Int, z: Int): BlockState? {
        val world = Stella.mc.world ?: return null
        return world.getBlockState(BlockPos(x, y, z))
    }

    fun getBlockAt(x: Int, y: Int, z: Int): Block? {
        val block = getBlockStateAt(x, y, z)?.block ?: return null
        return block
    }

    fun getBlockStringId(x: Int, y: Int, z: Int): String {
        val block = getBlockAt(x, y, z) ?: return "none"
        return Registries.BLOCK.getId(block).toString()
    }

    fun getBlockNumericId(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z)?: return -1
        return LegIDs.getLegacyId(state)
    }

    fun getPlayerByName(name: String): PlayerEntity? {
        val world = Stella.mc.world ?: return null
        return world.players.firstOrNull { it.name.string == name }
    }
}
