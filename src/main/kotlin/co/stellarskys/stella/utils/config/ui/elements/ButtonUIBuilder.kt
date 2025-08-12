package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Button
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.render.Render2D.width
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.dsl.*
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.PixelConstraint
import gg.essential.elementa.constraints.animation.Animations
import java.awt.Color

class ButtonUIBuilder {
    fun build(root: UIComponent, button: Button, window: Window): UIComponent {
        val container = UIRoundedRectangle(6f)
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0,0,0,0))
            .setChildOf(root)

        val name = UIText(button.name)
            .constrain {
                x = PixelConstraint(7f) // Moves text to the left
                y = CenterConstraint()
            }
            .setChildOf(container)

        val tooltip = createBlock(3f)
            .constrain {
                width = (button.description.width() + 10).pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = CenterConstraint() + 150.pixels()
            }
            .setColor(Color.black)
            .setChildOf(window)

        val tooltipText = UIText(button.description)
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setColor(Color.WHITE)
            .setChildOf(tooltip)

        tooltip.hide(true)

        name.onMouseEnter {
            tooltip.unhide(true)
        }

        name.onMouseLeave {
            tooltip.hide(true)
        }

        val buttonInput = createBlock(2f)
            .constrain {
                width = 40.pixels()
                height = 15.pixels()
                x = PixelConstraint(135f)  // Positions it on the right
                y = CenterConstraint()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(container)

        val buttonText = UIText(button.placeholder)
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(buttonInput)

        buttonInput.onMouseClick {
            button.onClick?.invoke()

            animate {
                setColorAnimation(
                    Animations.OUT_CUBIC,
                    0.1f,
                    Palette.Purple.toConstraint()
                )

                onComplete {
                    animate {
                        setColorAnimation(
                            Animations.IN_OUT_QUINT,
                            0.2f,
                            Palette.Purple.withAlpha(100).toConstraint()
                        )
                    }
                }
            }
        }

        return  container
    }
}