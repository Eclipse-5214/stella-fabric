package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.stripControlCodes

@Stella.Module
class dungeonBreakdown(): Feature("dungeonBreakdown", "catacombs") {
    override fun initialize() {
        val completeRegex = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")

        register<ChatEvent.Receive>({ event ->
            val msg = event.message.string.stripControlCodes()
            val match = completeRegex.find(msg)
            if (match == null) return@register

            ChatUtils.addMessage(Stella.PREFIX + "Cleared room counts:")


        })
    }
}