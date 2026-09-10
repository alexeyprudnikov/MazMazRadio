package com.example.mazmazradio.data

data class PlaylistPreset(
    val id: String,
    val title: String,
    val url: String
)

data class RadioStation(
    val title: String,
    val streamUrl: String,
    val category: String = "Разное",
    val logoUrl: String? = null,
    val isFavorite: Boolean = false
)

object AppConfig {
    val PRESET_PLAYLISTS = listOf(
        PlaylistPreset(
            id = "Russian",
            title = "Русское радио",
            url = "https://raw.githubusercontent.com/smolnp/IPTVru/refs/heads/gh-pages/IPRadio.m3u"
        )
    )
}