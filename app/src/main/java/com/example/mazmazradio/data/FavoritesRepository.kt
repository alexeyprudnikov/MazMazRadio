package com.example.mazmazradio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("radio_favorites_prefs", Context.MODE_PRIVATE)

    fun getFavoriteUrls(): Set<String> {
        return prefs.getStringSet("favorite_urls", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(streamUrl: String): Boolean {
        val currentFavorites = getFavoriteUrls().toMutableSet()
        val isNowFavorite = if (currentFavorites.contains(streamUrl)) {
            currentFavorites.remove(streamUrl)
            false
        } else {
            currentFavorites.add(streamUrl)
            true
        }
        prefs.edit {
            putStringSet("favorite_urls", currentFavorites)
        }
        return isNowFavorite
    }
}