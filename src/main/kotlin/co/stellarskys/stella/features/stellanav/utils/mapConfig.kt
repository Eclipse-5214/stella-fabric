package co.stellarskys.stella.features.stellanav.utils

import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import java.awt.Color

object mapConfig {
    val roomCheckmarks: Int get() = config["roomCheckmarks"] as? Int ?: 2
    val rcsize: Int get() = config["rcsize"] as? Int ?: 0
    val puzzleCheckmarks: Int get() = config["puzzleCheckmarks"] as? Int ?: 3
    val pcsize: Int get() = config["pcsize"] as? Int ?: 0
    val playerIcons: Boolean get() = config["mapEnabled"] as? Boolean ?: true

    val mapInfoUnder: Boolean get() = config["mapInfoUnder"] as? Boolean ?: true

    val noKey: Color get() = (config["noKeyColor"] as? RGBA)?.toColor() ?: Color.red
    val key: Color get() = (config["keyColor"] as? RGBA)?.toColor() ?: Color.green
    val doorLW: Double get() = (config["doorLineWidth"] as? Int ?: 3).toDouble()

    // map colors
    val NormalColor: Color get() = (config["normalRoomColor"] as? RGBA)?.toColor() ?: Color(107, 58, 17, 255)
    val PuzzleColor: Color get() = (config["puzzleRoomColor"] as? RGBA)?.toColor() ?: Color(117, 0, 133, 255)
    val TrapColor: Color get() = (config["trapRoomColor"] as? RGBA)?.toColor() ?: Color(216, 127, 51, 255)
    val MinibossColor: Color get() = (config["minibossRoomColor"] as? RGBA)?.toColor() ?: Color(254, 223, 0, 255)
    val BloodColor: Color get() = (config["bloodRoomColor"] as? RGBA)?.toColor() ?: Color(255, 0, 0, 255)
    val FairyColor: Color get() = (config["fairyRoomColor"] as? RGBA)?.toColor() ?: Color(224, 0, 255, 255)
    val EntranceColor: Color get() = (config["entranceRoomColor"] as? RGBA)?.toColor() ?: Color(20, 133, 0, 255)

    val NormalDoorColor: Color get() = (config["normalDoorColor"] as? RGBA)?.toColor() ?: Color(80, 40, 10, 255)
    val WitherDoorColor: Color get() = (config["witherDoorColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 255)
    val BloodDoorColor: Color get() = (config["bloodDoorColor"] as? RGBA)?.toColor() ?: Color(255, 0, 0, 255)
    val EntranceDoorColor: Color get() = (config["entranceDoorColor"] as? RGBA)?.toColor() ?: Color(0, 204, 0, 255)

    // other colors
    val mapBgColor: Color get() = (config["mapBgColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 100)
    val mapBorder: Boolean get() = config["mapBorder"] as? Boolean ?: true
    val mapBdColor: Color get() = (config["mapBdColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 255)
}
