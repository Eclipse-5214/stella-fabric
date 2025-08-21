package co.stellarskys.stella.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.File
import java.net.*

object NetworkUtils {
    fun createConnection(url: String, headers: Map<String, String> = emptyMap()): URLConnection {
        return URL(url).openConnection().apply {
            setRequestProperty("User-Agent", "Mozilla/5.0 (Zen)")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            connectTimeout = 10_000
            readTimeout = 30_000
        }
    }

    // Original: https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/utils/WebUtils.kt#L50
    inline fun <reified T> fetchJson(
        url: String,
        headers: Map<String, String> = emptyMap(),
        crossinline onSuccess: (T) -> Unit,
        crossinline onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers + ("Accept" to "application/json")) as HttpURLConnection
                connection.requestMethod = "GET"

                when (connection.responseCode) {
                    200 -> {
                        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val type = object : TypeToken<T>() {}.type
                        val data: T = Gson().fromJson(response, type)
                        onSuccess(data)
                    }
                    else -> throw HttpRetryableException("HTTP ${connection.responseCode}", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    fun getJson(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onSuccess: (JsonObject) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers + ("Accept" to "application/json")) as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonObject = parseToJsonElement(response).jsonObject
                    onSuccess(jsonObject)
                } else {
                    throw HttpRetryableException("HTTP ${connection.responseCode}", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    fun getText(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers) as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    onSuccess(response)
                } else {
                    throw HttpRetryableException("HTTP ${connection.responseCode}", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    fun downloadFile(
        url: String,
        outputFile: File,
        headers: Map<String, String> = emptyMap(),
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
        onComplete: (File) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers) as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val totalBytes = connection.contentLengthLong
                    var bytesDownloaded = 0L

                    connection.inputStream.use { input ->
                        outputFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                onProgress(bytesDownloaded, totalBytes)
                            }
                        }
                    }
                    onComplete(outputFile)
                } else {
                    throw HttpRetryableException("HTTP ${connection.responseCode}", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    fun postData(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap(),
        onSuccess: (String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers + ("Content-Type" to "application/json")) as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val response = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (connection.responseCode in 200..299) {
                    onSuccess(response)
                } else {
                    throw HttpRetryableException("HTTP ${connection.responseCode}: $response", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    fun putData(
        url: String,
        body: Any,
        headers: Map<String, String> = emptyMap(),
        onSuccess: (String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers + ("Content-Type" to "application/json")) as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.doOutput = true

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val response = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                if (connection.responseCode in 200..299) {
                    onSuccess(response)
                } else {
                    throw HttpRetryableException("HTTP ${connection.responseCode}: $response", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    fun headRequest(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onSuccess: (Map<String, List<String>>) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val connection = createConnection(url, headers) as HttpURLConnection
                connection.requestMethod = "HEAD"

                if (connection.responseCode in 200..299) {
                    onSuccess(connection.headerFields)
                } else {
                    throw HttpRetryableException("HTTP ${connection.responseCode}", connection.responseCode, connection.url)
                }
                connection.disconnect()
            }.onFailure { onError(it as? Exception ?: Exception(it)) }
        }
    }

    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        delay: Long = 1000,
        backoffMultiplier: Double = 2.0,
        operation: suspend () -> T
    ): T {
        var currentDelay = delay
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * backoffMultiplier).toLong()
            }
        }
        throw IllegalStateException("Retry failed")
    }

    fun isUrlReachable(url: String, timeoutMs: Int = 5000): Boolean {
        return runCatching {
            val connection = createConnection(url, headers = emptyMap()) as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.responseCode in 200..399
        }.getOrElse { false }
    }
}

data class HttpRetryableException(override val message: String, val code: Int, val url: URL) : Exception(message)