package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.ChatEvent
import co.stellarskys.stella.events.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.mapConfig
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.skyblock.dungeons.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import co.stellarskys.stella.utils.stripControlCodes

@Stella.Module
object boxWitherDoors: Feature("boxWitherDoors", "catacombs") {
    var keyObtained = false
    var bloodOpen = false

    val obtainKey = Regex("""^(?:\[[^]]+]\s)?(\w+) has obtained (Wither|Blood) Key!$""")
    val openedDoor = Regex("""^(\w+) opened a WITHER door!$""")
    val bloodOpened = Regex("""^The BLOOD DOOR has been opened!$""")

    override fun initialize() {
        register<ChatEvent.Receive>({ event ->
            val msg = event.message.string.stripControlCodes()

            val keyMatch = obtainKey.find(msg)
            if (keyMatch != null){
                keyObtained = true
                return@register
            }

            val doorMatch = openedDoor.find(msg)
            if (doorMatch != null){
                keyObtained = false
                return@register
            }

            val bloodMatch = bloodOpened.find(msg)
            if (bloodMatch != null){
                keyObtained = false
                bloodOpen = true
                return@register
            }

        })

        register<RenderEvent.World>({ event ->
            if(event.context == null || bloodOpen) return@register
            val color = if (keyObtained) mapConfig.key else mapConfig.noKey

            DungeonScanner.doors.forEach { door ->
                if (door == null) return@forEach
                if (door.state != DoorState.DISCOVERED) return@forEach
                if (door.type == DoorType.BLOOD && door.opened) return@forEach
                if (door.type !in setOf(DoorType.WITHER, DoorType.BLOOD)) return@forEach

                val (x, y, z) = door.getPos()

                if (door.rotation == 0) {
                    Render3D.renderBox(
                        event.context,
                        x.toDouble(), y.toDouble(), z.toDouble() + 0.5,
                        2.0, 3.0, 4.0,
                        color, true, mapConfig.doorLW
                    )
                } else {
                    Render3D.renderBox(
                        event.context,
                        x.toDouble() + 0.5, y.toDouble(), z.toDouble(),
                        3.0, 2.0, 4.0,
                        color, true, mapConfig.doorLW
                    )
                }
            }
        })
    }

    override fun onUnregister() {
        bloodOpen = false
        keyObtained = false
    }
}