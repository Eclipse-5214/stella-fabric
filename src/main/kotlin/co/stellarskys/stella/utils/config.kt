package co.stellarskys.stella.utils

import co.stellarskys.novaconfig.core.Config

val config = Config("Stella", "stella") {
    category("General"){
        textparagraph {
            configName = "info"
            name = "Stella"
            description = "Made by NEXD_"
        }
    }

    category( "Msc."){
        subcategory("Block Overlay")

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
            shouldShow {
                    settings -> settings["overlayEnabled"] as Boolean && settings["chromaHighlight"]as Boolean
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