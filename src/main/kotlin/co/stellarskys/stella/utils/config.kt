package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.hud.HUDEditor
import co.stellarskys.stella.utils.config.core.Config
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

val config = Config("Stella", "stella") {
    category("General"){
        subcategory("info") {
            textparagraph {
                configName = "info"
                name = "Stella"
                description = "Made by NEXD_"
            }

            button {
                configName = "website"
                name = "Website"
                description = "A link to stella's website"
            }
        }
    }

    category( "Dungeons") {
        subcategory( "Room Name") {
            toggle {
                configName = "showRoomName"
                name = "Show Room Name"
                description = "Showes the current dungeon rooms name in a hud"
            }
        }
    }

    category("StellaNav") {
        subcategory("general") {
            toggle {
                configName = "mapEnabled"
                name = "Enable Map"
                description = "Enables the dungeon map"
            }

            toggle {
                configName = "bossMapEnabled"
                name = "Enable Boss Map"
                description = "Enables the dungeon boss map"

                shouldShow { settings -> settings["mapEnabled"] as Boolean }
            }

            toggle {
                configName = "scoreMapEnabled"
                name = "Enable Score Map"
                description = "Enables the dungeon score map"
            }
        }

        subcategory("Map") {

        }

        subcategory("extra") {
            toggle {
                configName = "boxWitherDoors"
                name = "Box Wither Doors"
                description = "renders a box around wither doors"
            }

            toggle {
                configName = "seperateMapInfo"
                name = "Seprate Map Info"
                description = "renders the map info seperate from the dungeon map"
            }
        }
    }

    category( "Msc."){
        subcategory("Block Overlay") {

            toggle {
                configName = "overlayEnabled"
                name = "Render Block Overlay"
                description = "Highlights the block you are looking at"
            }

            colorpicker {
                configName = "blockHighlightColor"
                name = "Block Highlight Color"
                description = "The color to highlight blocks"
                default = rgba(0, 255, 255, 255)
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }

            toggle {
                configName = "fillBlockOverlay"
                name = "Fill blocks"
                description = "Fills the blocks with the color"
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }

            colorpicker {
                configName = "blockFillColor"
                name = "Block Fill Color"
                description = "The color to fill blocks"
                default = rgba(0, 255, 255, 30)
                shouldShow { settings -> settings["overlayEnabled"] as Boolean && settings["fillBlockOverlay"] as Boolean }
            }

            toggle {
                configName = "chromaHighlight"
                name = "Chroma overlay"
                description = "Makes the outline chroma"
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }

            stepslider {
                configName = "chromaOverlaySpeed"
                name = "Chroma Speed"
                description = "The speed of the chroma effect"
                min = 1
                max = 10
                step = 1
                default = 1
                shouldShow { settings ->
                    settings["overlayEnabled"] as Boolean && settings["chromaHighlight"] as Boolean
                }
            }

            stepslider {
                configName = "overlayLineWidth"
                name = "Line width"
                description = "Line width for the outline"
                min = 1
                max = 5
                step = 1
                default = 3
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }
        }
    }
}

@Stella.Command
object MainCommand : CommandUtils(
    "stella",
    listOf("sa", "sta")
) {
    override fun execute(context: CommandContext<FabricClientCommandSource>): Int {
        config.open()
        return 1
    }

    override fun buildCommand(builder: LiteralArgumentBuilder<FabricClientCommandSource>) {
        builder.then(
            ClientCommandManager.literal("hud")
                .executes { _ ->
                    TickUtils.schedule(1) {
                        Stella.mc.execute {
                            Stella.mc.setScreen(HUDEditor())
                        }
                    }
                    1
                }
        )
    }
}