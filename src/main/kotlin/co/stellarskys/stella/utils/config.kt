package co.stellarskys.stella.utils

import co.stellarskys.novaconfig.NovaApi

val config = NovaApi.createConfig("Stella", "stella"){
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
            shouldShow { it["overlayEnabled"] == true }
        }

        toggle {
            configName = "fillBlockOverlay"
            name = "Fill blocks"
            description = "Fills the blocks with the color"
            shouldShow { it["overlayEnabled"] == true }
        }

        colorpicker {
            configName = "blockFillColor"
            name = "Block Fill Color"
            description = "The color to fill blocks"
            default = rgba(0, 255, 255, 30)
            shouldShow {
                it["overlayEnabled"] == true && it["fillBlockOverlay"] == true
            }
        }

        toggle {
            configName = "chromaHighlight"
            name = "Chroma overlay"
            description = "Makes the outline chroma"
            shouldShow { it["overlayEnabled"] == true }
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
                it["overlayEnabled"] == true && it["chromaHighlight"] == true
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
            shouldShow { it["overlayEnabled"] == true }
        }
    }
}