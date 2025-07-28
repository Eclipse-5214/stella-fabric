package co.stellarskys.stella.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import co.stellarskys.stella.utils.TimeUtils.millis
import net.fabricmc.loader.api.FabricLoader
import java.awt.Color
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

class DataUtils<T: Any>(fileName: String, private val defaultObject: T) {
    companion object {
        private val gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color::class.java, object : JsonSerializer<Color>, JsonDeserializer<Color> {
                override fun serialize(
                    src: Color,
                    typeOfSrc: Type,
                    context: JsonSerializationContext
                ): JsonElement {
                    val obj = JsonObject()
                    obj.addProperty("r", src.red.toDouble())
                    obj.addProperty("g", src.green.toDouble())
                    obj.addProperty("b", src.blue.toDouble())
                    obj.addProperty("a", src.alpha.toDouble())
                    return obj
                }

                override fun deserialize(
                    json: JsonElement,
                    typeOfT: Type,
                    context: JsonDeserializationContext
                ): Color {
                    val obj = json.asJsonObject
                    val r = obj.get("r").asFloat.toInt()
                    val g = obj.get("g").asFloat.toInt()
                    val b = obj.get("b").asFloat.toInt()
                    val a = obj.get("a").asFloat.toInt()
                    return Color(r, g, b, a)
                }
            })
            .create()

        private val autosaveIntervals = ConcurrentHashMap<DataUtils<*>, Long>()
        private var loopStarted = false
    }

    private val dataFile = File(
        FabricLoader.getInstance().configDir.toFile(),
        "stella/${fileName}.json"
    )
    private var data: T = loadData()
    private var lastSavedTime = TimeUtils.now

    init {
        dataFile.parentFile.mkdirs()
        autosave(5)
        startAutosaveLoop()
    }

    private fun loadData(): T {
        return try {
            if (dataFile.exists()) {
                gson.fromJson(dataFile.readText(), defaultObject::class.java) ?: defaultObject
            } else defaultObject
        } catch (e: Exception) {
            println("Error loading data from ${'$'}{dataFile.absolutePath}: ${'$'}{e.message}")
            defaultObject
        }
    }

    @Synchronized
    fun save() {
        try {
            dataFile.writeText(gson.toJson(data))
        } catch (e: Exception) {
            println("Error saving data to ${'$'}{dataFile.absolutePath}: ${'$'}{e.message}")
            e.printStackTrace()
        }
    }

    fun autosave(intervalMinutes: Long = 5) {
        autosaveIntervals[this] = intervalMinutes * 60000
    }

    fun setData(newData: T) {
        data = newData
    }

    fun getData(): T = data

    private fun startAutosaveLoop() {
        if (loopStarted) return
        loopStarted = true
        LoopUtils.loop(10000) {
            autosaveIntervals.forEach { (dataUtils, interval) ->
                if (dataUtils.lastSavedTime.since.millis < interval) return@forEach
                try {
                    val currentData = dataUtils.loadData()
                    if (currentData == dataUtils.data) return@forEach
                } catch (ignored: Exception) {}
                dataUtils.save()
                dataUtils.lastSavedTime = TimeUtils.now
            }
        }
    }
}