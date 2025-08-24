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
                description = "Shows the current dungeon rooms name in a hud"
            }
        }
    }

    category("StellaNav") {

        subcategory("general") {
            toggle {
                configName = "mapEnabled"
                name = "Enable Map"
                description = "Enables the dungeon map"
                default = true
            }

            toggle {
                configName = "bossMapEnabled"
                name = "Enable Boss Map"
                description = "Enables the dungeon boss map"
                default = false
                shouldShow { settings -> settings["mapEnabled"] as Boolean }
            }

            toggle {
                configName = "scoreMapEnabled"
                name = "Enable Score Map"
                description = "Enables the dungeon score map"
                default = false
            }

            toggle {
                configName = "mapInfoUnder"
                name = "Map Info Under Map"
                description = "Renders map info below the map"
                default = true
            }
        }

        subcategory("Map") {
            colorpicker {
                configName = "mapBgColor"
                name = "Map Background Color"
                description = "Background color of the map"
                default = rgba(0, 0, 0, 100)
            }

            toggle {
                configName = "mapBorder"
                name = "Map Border"
                description = "Renders a border around the map"
                default = true
            }

            colorpicker {
                configName = "mapBdColor"
                name = "Map Border Color"
                description = "Color of the map border"
                default = rgba(0, 0, 0, 255)
                shouldShow { settings -> settings["mapBorder"] as Boolean }
            }

            stepslider {
                configName = "mapBdWidth"
                name = "Width"
                description = "The width of the map border"
                min = 1
                max = 5
                step = 1
                default = 2
                shouldShow { settings -> settings["mapBorder"] as Boolean }
            }

            dropdown {
                configName = "roomCheckmarks"
                name = "Room Checkmarks"
                description = "Style of room checkmarks"
                options = listOf("Checkmark", "Name", "Secrets", "Both")
                default = 0 // "Checkmark"
            }

            dropdown {
                configName = "puzzleCheckmarks"
                name = "Puzzle Checkmarks"
                description = "Style of puzzle checkmarks"
                options = listOf("Checkmark", "Name", "Secrets", "Both")
                default = 3 // "Both"
            }

            stepslider {
                configName = "rcsize"
                name = "Room Size"
                description = "Size of room checkmarks"
                min = 0
                max = 5
                step = 1
                default = 0
            }

            stepslider {
                configName = "pcsize"
                name = "Puzzle Size"
                description = "Size of puzzle checkmarks"
                min = 0
                max = 5
                step = 1
                default = 0
            }
        }

        subcategory("Room Colors") {
            colorpicker {
                configName = "normalRoomColor"
                name = "Normal"
                default = rgba(107, 58, 17, 255)
            }
            colorpicker {
                configName = "puzzleRoomColor"
                name = "Puzzle"
                default = rgba(117, 0, 133, 255)
            }
            colorpicker {
                configName = "trapRoomColor"
                name = "Trap"
                default = rgba(216, 127, 51, 255)
            }
            colorpicker {
                configName = "minibossRoomColor"
                name = "Miniboss"
                default = rgba(254, 223, 0, 255)
            }
            colorpicker {
                configName = "bloodRoomColor"
                name = "Blood"
                default = rgba(255, 0, 0, 255)
            }
            colorpicker {
                configName = "fairyRoomColor"
                name = "Fairy"
                default = rgba(224, 0, 255, 255)
            }
            colorpicker {
                configName = "entranceRoomColor"
                name = "Entrance"
                default = rgba(20, 133, 0, 255)
            }
        }

        subcategory("Door Colors") {
            colorpicker {
                configName = "normalDoorColor"
                name = "Normal Door"
                default = rgba(80, 40, 10, 255)
            }
            colorpicker {
                configName = "witherDoorColor"
                name = "Wither Door"
                default = rgba(0, 0, 0, 255)
            }
            colorpicker {
                configName = "bloodDoorColor"
                name = "Blood Door"
                default = rgba(255, 0, 0, 255)
            }
            colorpicker {
                configName = "entranceDoorColor"
                name = "Entrance Door"
                default = rgba(0, 204, 0, 255)
            }
        }

        subcategory("extra") {
            toggle {
                configName = "boxWitherDoors"
                name = "Box Wither Doors"
                description = "Renders a box around wither doors"
                default = false
            }

            colorpicker {
                configName = "keyColor"
                name = "Key Color"
                description = "Color for doors with keys"
                default = rgba(0, 255, 0, 255)
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            colorpicker {
                configName = "noKeyColor"
                name = "No Key Color"
                description = "Color for doors without keys"
                default = rgba(255, 0, 0, 255)
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            stepslider {
                configName = "doorLineWidth"
                name = "Door Line Width"
                description = "Line width for doors"
                min = 1
                max = 5
                step = 1
                default = 3
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            toggle {
                configName = "separateMapInfo"
                name = "Separate Map Info"
                description = "Renders the map info separate from the dungeon map"
                default = false
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