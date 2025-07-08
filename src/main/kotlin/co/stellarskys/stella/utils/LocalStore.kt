package co.stellarskys.stella.utils

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Paths

import co.stellarskys.stella.events.*

object LocalStores {
    private val instances = mutableSetOf<LocalStore>()

    fun register(store: LocalStore) {
        instances += store
    }

    fun saveAll() {
        instances.forEach { it.save() }
    }

    fun backupAll() {
        instances.forEach { it.saveBackup() }
    }

    fun get(moduleName: String): LocalStore? {
        return instances.find { it.moduleName == moduleName }
    }

    fun init() {
        EventBus.register<GameEvent.Unload>({
            saveAll()
        })

        EventBus.register<TickEvent.Client>({
            val now = System.currentTimeMillis()
            instances.forEach { store ->
                if (store.shouldBackup(now)) {
                    store.markBackup(now)
                    store.saveBackup()
                }
            }
        })
    }
}

class LocalStore(
    val moduleName: String,
    val filePath: String
) : MutableMap<String, Any?> by mutableMapOf() {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = Paths.get(filePath)
    private var lastBackupTimestamp: Long = 0L

    init {
        val loaded = load()
        if (loaded != null) this.putAll(loaded)
        LocalStores.register(this)
    }

    private fun load(): Map<String, Any?>? = runCatching {
        if (!Files.exists(file)) return null
        Files.newBufferedReader(file).use {
            gson.fromJson(it, MutableMap::class.java) as Map<String, Any?>
        }
    }.getOrNull()

    fun save() {
        Files.createDirectories(file.parent)
        Files.newBufferedWriter(file).use {
            gson.toJson(this, it)
        }
    }

    fun saveBackup() {
        val backup = Paths.get("config/stella/backup/$moduleName/${file.fileName}")
        Files.createDirectories(backup.parent)
        Files.newBufferedWriter(backup).use {
            gson.toJson(this, it)
        }
    }

    fun shouldBackup(now: Long = System.currentTimeMillis()): Boolean {
        return now - lastBackupTimestamp > 10 * 60 * 1000
    }

    fun markBackup(now: Long = System.currentTimeMillis()) {
        lastBackupTimestamp = now
    }
}

