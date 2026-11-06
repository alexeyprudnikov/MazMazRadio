package com.example.mazmazradio.data

/**
 * Источник станций. [url] — либо http(s)-ссылка на m3u, либо путь к asset-плейлисту
 * вида [ASSET_PREFIX] + "playlists/hostingradio.m3u".
 */
data class PlaylistPreset(
    val id: String,
    val title: String,
    val url: String
)

data class RadioStation(
    val title: String,
    val streamUrl: String,
    val category: String = Categories.DEFAULT,
    val isFavorite: Boolean = false
)

object AppConfig {
    /** Префикс для плейлистов, лежащих в app/src/main/assets. */
    const val ASSET_PREFIX = "file:///android_asset/"

    val PRESET_PLAYLISTS = listOf(
        PlaylistPreset(
            id = "hostingradio",
            title = "Онлайн-радио",          // категория по умолчанию для потоков без group-title
            url = ASSET_PREFIX + "playlists/hostingradio.m3u"
        )
    )
}