package com.example.mazmazradio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Избранное. Хранится набором ключей станций ([StationKey.of]), а не сырых URL:
 * поток может переехать на другой mount, и избранное по станции должно это пережить.
 *
 * Старые установки хранили сырой URL — при первом чтении набор нормализуется.
 */
class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("radio_favorites_prefs", Context.MODE_PRIVATE)

    init {
        normalizeStoredKeys()
    }

    /** Нормализованные ключи избранных станций. */
    fun getFavoriteKeys(): Set<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()

    fun isFavorite(streamUrl: String): Boolean =
        StationKey.of(streamUrl) in getFavoriteKeys()

    /** @return `true`, если станция добавлена в избранное, `false` — если убрана. */
    fun toggleFavorite(streamUrl: String): Boolean {
        val key = StationKey.of(streamUrl)
        val currentFavorites = getFavoriteKeys().toMutableSet()
        val isNowFavorite = if (currentFavorites.contains(key)) {
            currentFavorites.remove(key)
            false
        } else {
            currentFavorites.add(key)
            true
        }
        prefs.edit { putStringSet(KEY_FAVORITES, currentFavorites) }
        return isNowFavorite
    }

    private fun normalizeStoredKeys() {
        val stored = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: return
        val normalized = stored.mapTo(mutableSetOf()) { StationKey.of(it) }
        if (normalized != stored) {
            prefs.edit { putStringSet(KEY_FAVORITES, normalized) }
        }
    }

    private companion object {
        const val KEY_FAVORITES = "favorite_urls"
    }
}
