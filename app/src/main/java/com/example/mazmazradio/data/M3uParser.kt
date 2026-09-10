package com.example.mazmazradio.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class M3uParser {
    private val client = OkHttpClient()

    // Регулярные выражения для парсинга атрибутов
    private val logoRegex = """tvg-logo=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
    private val groupRegex = """group-title=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)

    suspend fun parsePlaylist(
        playlistUrl: String,
        defaultCategoryName: String = "Разное"
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<RadioStation>()

        try {
            val request = Request.Builder().url(playlistUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext emptyList()
            val content = response.body?.string() ?: return@withContext emptyList()

            var currentTitle = "Unknown Station"
            var currentLogo: String? = null
            var currentCategory: String? = null

            content.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("#EXTINF:", ignoreCase = true) -> {
                        // 1. Извлекаем логотип
                        logoRegex.find(trimmed)?.let { match ->
                            currentLogo = match.groupValues[1].ifBlank { null }
                        }

                        // 2. Извлекаем группу (категорию)
                        groupRegex.find(trimmed)?.let { match ->
                            currentCategory = match.groupValues[1].ifBlank { null }
                        }

                        // 3. Извлекаем название станции (всё, что после последней запятой)
                        val commaIdx = trimmed.lastIndexOf(',')
                        if (commaIdx != -1) {
                            currentTitle = trimmed.substring(commaIdx + 1).trim()
                        }
                    }
                    // Альтернативный тег категории, который иногда встречается в M3U
                    trimmed.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        val group = trimmed.substringAfter("#EXTGRP:").trim()
                        if (group.isNotEmpty()) {
                            currentCategory = group
                        }
                    }
                    // Это URL потока
                    trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                        stations.add(
                            RadioStation(
                                title = currentTitle,
                                streamUrl = trimmed,
                                category = currentCategory ?: defaultCategoryName,
                                logoUrl = currentLogo
                            )
                        )
                        // Сбрасываем временные переменные для следующей станции
                        currentTitle = "Unknown Station"
                        currentLogo = null
                        currentCategory = null
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("M3uParser", "Ошибка загрузки плейлиста: ${e.localizedMessage}", e)
        }

        return@withContext stations
    }
}