package com.example.mazmazradio.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Читает и разбирает m3u-плейлист.
 *
 * Источник — asset-плейлист (`file:///android_asset/...`, см. [AppConfig.ASSET_PREFIX])
 * либо http(s) ссылка. Название берётся после запятой в `#EXTINF`, категория — из
 * `group-title`, пустая категория → [Categories.DEFAULT].
 */
class M3uParser(private val appContext: Context? = null) {
    private val client = OkHttpClient()

    private val groupRegex = """group-title=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)

    suspend fun parsePlaylist(
        playlistUrl: String,
        defaultCategoryName: String = Categories.DEFAULT
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val content = readSource(playlistUrl) ?: return@withContext emptyList()
        parseContent(content, defaultCategoryName)
    }

    /** Разбор готового текста плейлиста (для встроенных списков и тестов). */
    fun parseContent(
        content: String,
        defaultCategoryName: String = Categories.DEFAULT
    ): List<RadioStation> {
        val stations = mutableListOf<RadioStation>()

        var currentTitle = "Unknown Station"
        var currentCategory: String? = null

        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:", ignoreCase = true) -> {
                    // 1. Группа (категория)
                    groupRegex.find(trimmed)?.let { match ->
                        currentCategory = match.groupValues[1].ifBlank { null }
                    }

                    // 2. Название станции — всё после запятой, которая закрывает атрибуты
                    currentTitle = extractTitle(trimmed)
                }
                // Это URL потока
                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    stations.add(
                        RadioStation(
                            title = currentTitle,
                            streamUrl = trimmed,
                            category = currentCategory ?: defaultCategoryName
                        )
                    )
                    // Сбрасываем временные переменные для следующей станции
                    currentTitle = "Unknown Station"
                    currentCategory = null
                }
            }
        }

        return stations
    }

    /**
     * Название — часть строки после запятой, закрывающей блок атрибутов.
     * Обычный `lastIndexOf(',')` ломается на запятых внутри title, а `firstIndexOf` —
     * на запятых внутри атрибутов (`group-title="Юмор, спорт"`).
     */
    private fun extractTitle(line: String): String {
        val rest = line.substringAfter("#EXTINF:", line)
        var inQuotes = false
        var quoteChar = ' '
        for (index in rest.indices) {
            val char = rest[index]
            when {
                inQuotes && char == quoteChar -> inQuotes = false
                !inQuotes && (char == '"' || char == '\'') -> {
                    inQuotes = true
                    quoteChar = char
                }

                !inQuotes && char == ',' -> return rest.substring(index + 1).trim()
            }
        }
        return rest.trim()
    }

    private fun readSource(url: String): String? = when {
        url.startsWith(AppConfig.ASSET_PREFIX) ||
            url.startsWith("asset://", ignoreCase = true) -> readAsset(url)

        else -> readHttp(url)
    }

    private fun readAsset(url: String): String? {
        val context = appContext ?: return null
        val assetPath = url
            .removePrefix(AppConfig.ASSET_PREFIX)
            .let { if (it.startsWith("asset://", ignoreCase = true)) it.removePrefix("asset://") else it }
            .trimStart('/')
        return try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            android.util.Log.e("M3uParser", "Ошибка чтения asset-плейлиста '$assetPath'", e)
            null
        }
    }

    private fun readHttp(url: String): String? = try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    } catch (e: Exception) {
        android.util.Log.e("M3uParser", "Ошибка загрузки плейлиста: ${e.localizedMessage}", e)
        null
    }
}
