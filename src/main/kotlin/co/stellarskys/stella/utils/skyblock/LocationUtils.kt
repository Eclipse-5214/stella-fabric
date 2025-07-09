package co.stellarskys.stella.utils.skyblock

import co.stellarskys.stella.events.*
import co.stellarskys.stella.utils.Utils.removeEmotes
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.TeamS2CPacket

object LocationUtils {
    private val areaRegex = "^(?:Area|Dungeon): ([\\w ]+)$".toRegex()
    private val subAreaRegex = "^ ([⏣ф]) .*".toRegex()
    private var cachedAreas = mutableMapOf<String?, Boolean>()
    private var cachedSubareas = mutableMapOf<String?, Boolean>()
    var area: String? = null
    var subarea: String? = null

    init {
        println("locationUtils initilaizing")
        EventBus.register<PacketEvent.Received> ({ event ->
            when (val packet = event.packet) {
                is PlayerListS2CPacket -> {
                    val action = packet.actions.firstOrNull()
                    if (action != PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME && action != PlayerListS2CPacket.Action.ADD_PLAYER) return@register
                    packet.entries?.forEach { entry ->
                        val displayName = entry.displayName?.string ?: return@forEach

                        val line = displayName.removeEmotes()
                        val match = areaRegex.find(line) ?: return@forEach
                        val newArea = match.groupValues[1]
                        if (newArea.lowercase() != area) {
                            EventBus.post(AreaEvent.Main(newArea))
                            area = newArea.lowercase()
                        }
                    }
                }
                is TeamS2CPacket -> {
                    val teamData = packet.team.orElse(null) ?: return@register
                    val prefix = teamData.prefix?.string ?: ""
                    val suffix = teamData.suffix?.string ?: ""
                    if (prefix.isEmpty() || suffix.isEmpty()) return@register

                    val line = prefix + suffix
                    if (!subAreaRegex.matches(line)) return@register
                    if (line.lowercase() != subarea) {
                        EventBus.post(AreaEvent.Sub(line))
                        subarea = line.lowercase()
                    }
                }
            }
        })

        EventBus.register<AreaEvent.Main> ({
            cachedAreas.clear()
        })
        EventBus.register<AreaEvent.Sub> ({
            cachedSubareas.clear()
        })
    }

    fun checkArea(areaLower: String?): Boolean {
        return cachedAreas.getOrPut(areaLower) {
            areaLower?.let { area == it } ?: true
        }
    }

    fun checkSubarea(subareaLower: String?): Boolean {
        return cachedSubareas.getOrPut(subareaLower) {
            subareaLower?.let { subarea?.contains(it) == true } ?: true
        }
    }
}