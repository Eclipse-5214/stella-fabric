package co.stellarskys.stella.utils.config.core

import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.markdown.MarkdownComponent
import net.minecraft.client.MinecraftClient
import kotlinx.serialization.json.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import java.awt.Color
import java.io.File


//Main config Shananagens
class Config(
    configFileName: String,
    modID: String,
    theme: File? = null,
    file: File? = null,
    builder: Config.() -> Unit
) {
    private val categories = mutableMapOf<String, ConfigCategory>()

    private val fileName = configFileName
    private val configPath = file
    private val mod = modID
    private var loaded = false

    private var configUI: WindowScreen? = null
    private var selectedCategory: ConfigCategory? = null
    private val elementContainers = mutableMapOf<String, UIComponent>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private val listeners = mutableListOf<(configName: String, value: Any?) -> Unit>()

    private var categoryX = 10

    private var needsVisibilityUpdate = false

    private val resolvedFile: File
        get() = configPath ?: File("config/$mod/settings.json")

    init {
        this.builder()
        selectedCategory = categories.values.firstOrNull()
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ -> save() }
    }

    // DSL functions
    fun category(name: String, builder: ConfigCategory.() -> Unit) {
        categories[name] = ConfigCategory(name).apply(builder)
    }

    fun markdowncategory(name: String, markdown: String){
        categories[name] = MarkdownCategory(name, markdown)
    }

    // UI builders
    private fun buildUI(initial: Boolean){
        configUI = object: WindowScreen(ElementaVersion.V2) {
            init {
                categories.entries.forEachIndexed { index, category ->
                    val container = UIBlock()
                        .constrain {
                            width = 100.pixels()
                            height = 20.pixels()
                            x = categoryX.pixels()
                            y = 10.pixels()
                        }
                        .setColor(Palette.Mauve)
                        .setChildOf(window)

                    // Category label text
                    val label = UIWrappedText(category.key, centered = true)
                        .constrain {
                            x = CenterConstraint()
                            y = CenterConstraint()
                            width = 96.pixels()
                        }
                        .setColor(Color.WHITE)
                        .setChildOf(container)

                    // Click handler to change category view

                }
            }

            override fun shouldPause(): Boolean = false
        }
    }

    private fun buildMarkown(root: UIComponent, category: ConfigCategory){
        val catagoryContainer = ScrollComponent()
            .constrain {
                width = 450.pixels()
                height = 325.pixels()
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(root)

        val markdown = MarkdownComponent(category.markdown)
            .constrain {
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f)
                x = CenterConstraint()
                y = PixelConstraint(2f)
            }
            .setChildOf(catagoryContainer)
    }

    private fun buildCategory(root: UIComponent, category: ConfigCategory){
        val catagoryContainer = ScrollComponent()
            .constrain {
                width = 450.pixels()
                height = 325.pixels()
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(root)

        category.elements.entries.forEachIndexed { index, (key, element) ->
            val component = when (element) {
                /*
                is Button -> ButtonUIBuilder().build(catagoryContainer, element)
                is ColorPicker -> ColorPickerUIBuilder().build(catagoryContainer, element)
                is Dropdown -> DropdownUIBuilder().build(catagoryContainer, element)
                is Keybind -> KeybindUIBuilder().build(catagoryContainer, element)
                is Slider -> SliderUIBuilder().build(catagoryContainer, element)
                is StepSlider -> StepSliderUIBuilder().build(catagoryContainer, element)
                is Subcategory -> SubcategoryUIBuilder().build(catagoryContainer, element)
                is TextInput -> TextInputUIBuilder().build(catagoryContainer, element)
                is TextParagraph -> TextParagraphUIBuilder().build(catagoryContainer, element)
                is Toggle -> ToggleUIBuilder().build(catagoryContainer, element, this)
                 */

                else -> null
            }

            component!!.constrain {
                x = CenterConstraint()
                y = SiblingConstraint(5f)
            }

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element
        }

        needsVisibilityUpdate = true
        scheduleVisibilityUpdate()
    }

    // UI functions
    fun open() {
        buildIfNeeded()
        TickUtils.schedule(1){
            MinecraftClient.getInstance().setScreen(configUI)
        }
    }

    private fun buildIfNeeded(){
        if (configUI == null) {
            ensureLoaded()
            buildUI(true)
        }
    }

    private fun scheduleVisibilityUpdate() {
        if (!needsVisibilityUpdate) return

        elementContainers.keys.forEach { key ->
            val element = elementRefs[key] ?: return@forEach
            val visible = element.isVisible(this)

            updateElementVisibility(key)
        }

        needsVisibilityUpdate = false
    }

    private fun updateElementVisibility(configKey: String) {
        val container = elementContainers[configKey] ?: return
        val element = elementRefs[configKey] ?: return
        val visible = element.isVisible(this)

        if (visible) container.unhide(true) else container.hide(true)
    }

    // Helper functions
    fun flattenValues(): Map<String, Any?> {
        return categories
            .flatMap { it.value.elements.values }
            .associate { it.configName to it.value }
    }

    fun registerListener(callback: (configName: String, value: Any?) -> Unit) {
        listeners += callback
    }

    internal fun notifyListeners(configName: String, newValue: Any?) {
        listeners.forEach { it(configName, newValue) }
        updateConfig()
    }

    private fun updateConfig() {
        needsVisibilityUpdate = true
        scheduleVisibilityUpdate()
    }

    private fun toJson(): JsonObject {
        return buildJsonObject {
            categories.forEach { (_, category) ->
                val categoryValues = buildJsonObject {
                    category.elements.forEach { (_, element) ->
                        val id = element.configName
                        val value = element.value

                        if (id.isNotBlank() && value != null) {
                            val jsonValue = when (value) {
                                is Boolean -> JsonPrimitive(value)
                                is Int -> JsonPrimitive(value)
                                is Float -> JsonPrimitive(value)
                                is Double -> JsonPrimitive(value)
                                is String -> JsonPrimitive(value)
                                is RGBA -> JsonPrimitive(value.toHex())
                                else -> {
                                    println("Unsupported type for $id: ${value::class.simpleName}")
                                    return@forEach
                                }
                            }

                            put(id, jsonValue)
                        }
                    }
                }

                if (categoryValues.isNotEmpty()) {
                    put(category.name, categoryValues)
                }
            }
        }
    }

    private fun fromJson(json: JsonObject) {
        categories.forEach { (_, category) ->
            val categoryData = json[category.name]?.jsonObject ?: return@forEach

            category.elements.forEach { (_, element) ->
                val id = element.configName
                val jsonValue = categoryData[id] ?: return@forEach

                val newValue = when (val current = element.value) {
                    is Boolean -> jsonValue.jsonPrimitive.booleanOrNull
                    is Int -> jsonValue.jsonPrimitive.intOrNull
                    is Float -> jsonValue.jsonPrimitive.floatOrNull
                    is Double -> jsonValue.jsonPrimitive.doubleOrNull
                    is String -> jsonValue.jsonPrimitive.contentOrNull
                    is RGBA -> jsonValue.jsonPrimitive.contentOrNull?.let { RGBA.fromHex(it) }
                    else -> {
                        println("Skipping unsupported load type for '$id': ${current?.let { it::class.simpleName } ?: "null"}")
                        null
                    }
                }

                if (newValue != null) element.value = newValue
            }
        }
    }

    fun save(){
        try {
            val target = resolvedFile
            target.parentFile?.mkdirs()

            val json = toJson()

            val jsonOutput = Json {
                prettyPrint = true
            }

            val jsonString = jsonOutput.encodeToString(JsonObject.serializer(), json)
            target.writeText(jsonString)

        } catch (e: Exception) {
            println("Failed to save config for '$mod': ${e.message}")
            e.printStackTrace()
        }
    }

    fun load() {
        try {
            val target = resolvedFile
            if (!target.exists()) return

            val jsonText = target.readText()
            val loadedJson = Json.decodeFromString(JsonObject.serializer(), jsonText)

            // Inject into config
            fromJson(loadedJson)

        } catch (e: Exception) {
            println("Failed to load config for '$mod': ${e.message}")
            e.printStackTrace()
        }
    }

    fun ensureLoaded() {
        if (!loaded) {
            load()
            loaded = true
        }
    }

    // get functions
    operator fun get(key: String): Any {
        ensureLoaded()
        return flattenValues()[key]
            ?: error("No config entry found for key '$key'")
    }

    inline operator fun <reified T> Config.get(key: String): T {
        ensureLoaded()
        val value = flattenValues()[key]
            ?: error("No config entry found for key '$key'")

        return value as? T
            ?: error("Config value for '$key' is not of expected type ${T::class.simpleName}")
    }

    inline fun <reified T> getValue(key: String): T {
        ensureLoaded()
        val value = flattenValues()[key]
            ?: error("Missing config value for '$key'")

        return value as? T
            ?: error("Config value for '$key' is not of type ${T::class.simpleName}")
    }
}
