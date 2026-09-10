package com.example.mazmazradio.ui

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.mazmazradio.data.*
import com.example.mazmazradio.service.RadioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit

sealed class RadioUiState {
    object Loading : RadioUiState()
    data class Success(val categories: Map<String, List<RadioStation>>) : RadioUiState()
    data class Error(val message: String) : RadioUiState()
}

class RadioViewModel : ViewModel() {

    private val parser = M3uParser()
    private var favoritesRepository: FavoritesRepository? = null
    private var sharedPreferences: android.content.SharedPreferences? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Кешируем полный список всех загруженных станций
    private var rawStationsList = listOf<RadioStation>()

    private val _uiState = MutableStateFlow<RadioUiState>(RadioUiState.Loading)
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sleepTimerRemainingMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingMinutes: StateFlow<Int?> = _sleepTimerRemainingMinutes.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        loadAllPresetPlaylists()
    }

    fun initMediaController(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("radio_prefs", Context.MODE_PRIVATE)
        }
        // Инициализируем репозиторий избранного при наличии контекста
        if (favoritesRepository == null) {
            favoritesRepository = FavoritesRepository(context.applicationContext)
            // Если станции уже были загружены, обновляем группировку с учётом избранного
            if (rawStationsList.isNotEmpty()) {
                updateUiStateWithFavorites()
            }
        }
        if (mediaController != null) return
        val sessionToken = SessionToken(context, ComponentName(context, RadioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            }
        })
    }

    fun loadAllPresetPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = RadioUiState.Loading

            val deferredList = AppConfig.PRESET_PLAYLISTS.map { preset ->
                async { parser.parsePlaylist(preset.url, preset.title) }
            }

            val results = deferredList.awaitAll()
            val allStations = results.flatten()

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                if (allStations.isEmpty()) {
                    _uiState.value = RadioUiState.Error("Не удалось загрузить станции из плейлистов. Проверьте подключение к сети.")
                } else {
                    rawStationsList = allStations
                    updateUiStateWithFavorites()

                    // Восстанавливаем последнюю станцию на паузе
                    restoreLastStation(allStations)
                }
            }
        }
    }

    private fun restoreLastStation(stations: List<RadioStation>) {
        val lastUrl = sharedPreferences?.getString("last_station_url", null) ?: return
        val lastStation = stations.find { it.streamUrl == lastUrl }
        if (lastStation != null) {
            prepareStationWithoutAutoPlay(lastStation)
        }
    }

    // Добавление / удаление из Избранного
    fun toggleFavorite(station: RadioStation) {
        val repo = favoritesRepository ?: return
        repo.toggleFavorite(station.streamUrl)
        updateUiStateWithFavorites()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        updateUiStateWithFavorites()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingMinutes.value = null
            return
        }

        _sleepTimerRemainingMinutes.value = minutes
        sleepTimerJob = viewModelScope.launch {
            var current = minutes
            while (current > 0) {
                delay(60_000L.milliseconds) // Ждем 1 минуту
                current--
                _sleepTimerRemainingMinutes.value = if (current > 0) current else null
            }
            // По истечении времени ставим на паузу
            mediaController?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMinutes.value = null
    }

    // Вспомогательная функция группировки
    private fun updateUiStateWithFavorites() {
        val favoriteUrls = favoritesRepository?.getFavoriteUrls() ?: emptySet()
        val query = _searchQuery.value.trim().lowercase()

        // 1. Проставляем флаги избранного
        val updatedList = rawStationsList.map { station ->
            station.copy(isFavorite = favoriteUrls.contains(station.streamUrl))
        }

        // 2. Фильтруем по поисковому запросу
        val filteredList = if (query.isEmpty()) {
            updatedList
        } else {
            updatedList.filter {
                it.title.lowercase().contains(query) || it.category.lowercase().contains(query)
            }
        }

        val categoriesMap = mutableMapOf<String, List<RadioStation>>()

        // 3. Избранное выводим только если поиск пустой или если среди найденного есть избранные
        val favoritesList = filteredList.filter { it.isFavorite }
        if (favoritesList.isNotEmpty() && query.isEmpty()) {
            categoriesMap["★ Избранное"] = favoritesList
        }

        // 4. Группируем по категориям
        val groupedByCategory = filteredList.groupBy { it.category }
        categoriesMap.putAll(groupedByCategory)

        val newState = RadioUiState.Success(categoriesMap)
        _uiState.value = newState
    }

    fun playStation(station: RadioStation) {
        _currentStation.value = station
        saveLastStationUrl(station.streamUrl)

        mediaController?.run {
            val metadata = MediaMetadata.Builder()
                .setTitle(station.title)
                .setArtist("Online Radio")
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(station.streamUrl)
                .setMediaMetadata(metadata)
                .build()

            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    private fun prepareStationWithoutAutoPlay(station: RadioStation) {
        _currentStation.value = station
        mediaController?.run {
            val metadata = MediaMetadata.Builder()
                .setTitle(station.title)
                .setArtist("Online Radio")
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(station.streamUrl)
                .setMediaMetadata(metadata)
                .build()

            setMediaItem(mediaItem)
            prepare() // Только подготавливаем поток, без play()
        }
    }

    private fun saveLastStationUrl(url: String) {
        sharedPreferences?.edit(commit = false) {
            putString("last_station_url", url)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}