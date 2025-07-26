package co.stellarskys.stella.hud

import co.stellarskys.stella.utils.LocalStore

class HUDDataStore(module: String) {
    private val store = LocalStore(module, "config/stella/$module.json")

    fun getPositions(): HUDPositions {
        val raw = store["positions"]
        return if (raw is Map<*, *>) {
            val parsed = raw.mapNotNull { (k, v) ->
                if (k is String && v is Map<*, *>) {
                    val x = (v["x"] as? Number)?.toFloat() ?: 0f
                    val y = (v["y"] as? Number)?.toFloat() ?: 0f
                    val scale = (v["scale"] as? Number)?.toFloat() ?: 1f
                    val enabled = (v["enabled"] as? Boolean) ?: true
                    k to HUDPosition(x, y, scale, enabled)
                } else null
            }.toMap()
            HUDPositions(parsed.toMutableMap())
        } else {
            HUDPositions()
        }
    }

    fun savePositions(data: HUDPositions) {
        store["positions"] = data.positions
        store.save()
    }
}

data class HUDPosition(var x: Float, var y: Float, var scale: Float = 1f, var enabled: Boolean = true)

data class HUDPositions(val positions: MutableMap<String, HUDPosition> = mutableMapOf())
