package com.example.mazmazradio.ui

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.mazmazradio.data.*
import com.example.mazmazradio.service.RadioService
import com.example.mazmazradio.service.SleepTimer
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

sealed class RadioUiState {
    object Loading : RadioUiState()
    data class Success(val categories: Map<String, List<RadioStation>>) : RadioUiState()
    data class Error(val message: String) : RadioUiState()
}

class RadioViewModel : ViewModel() {

    private var appContext: Context? = null
    private var parser = M3uParser()
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

    /** Человекочитаемая ошибка воспроизведения; `null` — всё в порядке. */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    /** Остаток таймера сна в секундах. */
    val sleepTimerRemainingSeconds: StateFlow<Int?> = SleepTimer.remainingSeconds

    fun initMediaController(context: Context) {
        // Источник может лежать в assets — парсеру нужен контекст.
        if (appContext == null) {
            appContext = context.applicationContext
            parser = M3uParser(appContext)
        }
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
        ensurePlaylistsLoaded()

        if (mediaController != null) return
        val sessionToken = SessionToken(context, ComponentName(context, RadioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupPlayerListener()
            syncPlaybackState()
        }, MoreExecutors.directExecutor())
    }

    /** Станции ещё не загружены (например, парсер ждал контекст) — грузим. */
    fun ensurePlaylistsLoaded() {
        if (rawStationsList.isEmpty() && _uiState.value is RadioUiState.Loading) {
            loadAllPresetPlaylists()
        }
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) _playbackError.value = null
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) _playbackError.value = null
            }

            override fun onPlayerError(error: PlaybackException) {
                // Переподключением занимается сервис (RadioService), здесь только показ.
                _playbackError.value = describe(error)
            }
        })
    }

    /** Состояние плеера могло измениться, пока контроллера ещё не было. */
    private fun syncPlaybackState() {
        val controller = mediaController ?: return
        _isPlaying.value = controller.isPlaying
        _isBuffering.value = controller.playbackState == Player.STATE_BUFFERING
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
                    _uiState.value = RadioUiState.Error(
                        "Не удалось загрузить плейлист. Проверьте, что источник доступен: " +
                            AppConfig.PRESET_PLAYLISTS.joinToString { it.url }
                    )
                } else {
                    rawStationsList = allStations
                    updateUiStateWithFavorites()

                    // Показываем станцию, на которой всё закончилось, но эфир не включаем.
                    restoreLastStation(allStations)
                }
            }
        }
    }

    private fun restoreLastStation(stations: List<RadioStation>) {
        if (_currentStation.value != null) return
        val lastKey = sharedPreferences?.getString(KEY_LAST_STATION, null)
            // До ключей станций здесь лежал сырой URL.
            ?: sharedPreferences?.getString("last_station_url", null)?.let { StationKey.of(it) }
            ?: return
        _currentStation.value = stations.firstOrNull { StationKey.of(it.streamUrl) == lastKey }
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

    fun setSleepTimer(minutes: Int) = SleepTimer.arm(minutes)

    fun cancelSleepTimer() = SleepTimer.cancel()

    // Вспомогательная функция группировки
    private fun updateUiStateWithFavorites() {
        val repo = favoritesRepository
        val query = _searchQuery.value.trim().lowercase()

        // 1. Проставляем флаги избранного
        val updatedList = rawStationsList.map { station ->
            station.copy(isFavorite = repo?.isFavorite(station.streamUrl) == true)
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
            categoriesMap[Categories.FAVORITES] = favoritesList
        }

        // 4. Группируем по категориям и расставляем их в порядке Categories.ORDER
        val groupedByCategory = filteredList.groupBy { it.category }
            .toSortedMap(compareBy({ Categories.rank(it) }, { it }))
        categoriesMap.putAll(groupedByCategory)

        val newState = RadioUiState.Success(categoriesMap)
        _uiState.value = newState
    }

    fun playStation(station: RadioStation) {
        _currentStation.value = station
        _playbackError.value = null
        saveLastStation(station)

        mediaController?.run {
            setMediaItem(mediaItemOf(station))
            prepare()
            play()
        }
    }

    /**
     * Повтор после ошибки: сервис уже исчерпал автоповтор, а ручная кнопка
     * должна работать всегда, поэтому готовим поток прямо отсюда.
     */
    fun retryNow() {
        _playbackError.value = null
        mediaController?.let { controller ->
            if (controller.mediaItemCount == 0) {
                _currentStation.value?.let { playStation(it) }
            } else {
                controller.prepare()
                controller.play()
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            // После холодного старта станция показана, но поток не подготовлен.
            _currentStation.value?.let { playStation(it) }
            return
        }
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    private fun mediaItemOf(station: RadioStation): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(station.title)
            .setArtist("Online Radio")
            .build()

        return MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun saveLastStation(station: RadioStation) {
        val key = StationKey.of(station.streamUrl)
        sharedPreferences?.edit(commit = false) {
            putString(KEY_LAST_STATION, key)
        }
    }

    private fun describe(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Нет сети — эфир не отвечает"

        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "Поток недоступен (${error.errorCodeName})"

        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
            "Отстали от прямого эфира — переподключаемся"

        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
            "Поток сломан или не аудио (${error.errorCodeName})"

        else -> "Не удалось включить эфир (${error.errorCodeName})"
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    private companion object {
        const val KEY_LAST_STATION = "last_station_key"
    }
}
