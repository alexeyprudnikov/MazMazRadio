package com.example.mazmazradio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mazmazradio.data.RadioStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: RadioViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val remainingTimer by viewModel.sleepTimerRemainingMinutes.collectAsState()

    var showFullPlayer by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Поиск станции...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (searchQuery.isNotEmpty()) {
                                            viewModel.onSearchQueryChanged("")
                                        } else {
                                            isSearchActive = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                                    }
                                }
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "MAZMAZ RADIO",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск")
                            }
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                BadgedBox(
                                    badge = {
                                        if (remainingTimer != null) {
                                            Badge { Text("${remainingTimer}m") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NightlightRound,
                                        contentDescription = "Таймер сна",
                                        tint = if (remainingTimer != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                currentStation?.let { station ->
                    MiniPlayer(
                        station = station,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onBarClick = { showFullPlayer = true }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val state = uiState) {
                    is RadioUiState.Loading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Загрузка станций...", color = Color.Gray)
                        }
                    }
                    is RadioUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is RadioUiState.Success -> {
                        StationGroupList(
                            categories = state.categories,
                            currentStation = currentStation,
                            isPlaying = isPlaying,
                            isSearching = searchQuery.isNotEmpty(),
                            onStationClick = { station -> viewModel.playStation(station) },
                            onFavoriteClick = { station -> viewModel.toggleFavorite(station) }
                        )
                    }
                }
            }
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentTimerMinutes = remainingTimer,
                onSelectOption = { minutes ->
                    viewModel.setSleepTimer(minutes)
                    showSleepTimerDialog = false
                },
                onCancelTimer = {
                    viewModel.cancelSleepTimer()
                    showSleepTimerDialog = false
                },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        if (showFullPlayer && currentStation != null) {
            ModalBottomSheet(
                onDismissRequest = { showFullPlayer = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                FullPlayerView(
                    station = currentStation!!,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    remainingTimer = remainingTimer,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onOpenTimerDialog = { showSleepTimerDialog = true }
                )
            }
        }
    }
}

@Composable
fun StationGroupList(
    categories: Map<String, List<RadioStation>>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    isSearching: Boolean,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (categoryName, stations) ->
            val isExpanded = if (isSearching) true else (expandedStates[categoryName] ?: false)

            item(key = categoryName) {
                CategoryAccordionHeader(
                    categoryName = categoryName,
                    stationCount = stations.size,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedStates[categoryName] = !isExpanded
                    }
                )
            }

            if (isExpanded) {
                items(
                    items = stations,
                    key = { it.streamUrl }
                ) { station ->
                    val isSelected = station == currentStation
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        StationCard(
                            station = station,
                            isSelected = isSelected,
                            isPlaying = isSelected && isPlaying,
                            onClick = { onStationClick(station) },
                            onFavoriteClick = { onFavoriteClick(station) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryAccordionHeader(
    categoryName: String,
    stationCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "($stationCount)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StationCard(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(
                logoUrl = station.logoUrl,
                contentDescription = station.title,
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray
                )
            }

            if (isPlaying) {
                EqualizerAnimation()
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Кнопка избранного (звёздочка)
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Избранное",
                    tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    station: RadioStation,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onBarClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(
                logoUrl = station.logoUrl,
                contentDescription = station.title,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (isBuffering) "Буферизация..." else "ПРЯМОЙ ЭФИР",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBuffering) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(42.dp)
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun FullPlayerView(
    station: RadioStation,
    isPlaying: Boolean,
    isBuffering: Boolean,
    remainingTimer: Int?,
    onPlayPauseClick: () -> Unit,
    onOpenTimerDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StationLogo(
            logoUrl = station.logoUrl,
            contentDescription = station.title,
            modifier = Modifier.size(220.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = station.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = station.category,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Кнопка настройки Таймера сна:
        OutlinedButton(onClick = onOpenTimerDialog) {
            Icon(Icons.Default.NightlightRound, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (remainingTimer != null) "Таймер: $remainingTimer мин." else "Таймер сна")
        }

        Spacer(modifier = Modifier.height(20.dp))

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            if (isBuffering) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EqualizerAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val heights = listOf(
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "1"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "2"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 1.0f,
            animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "3"
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(anim.value)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentTimerMinutes: Int?,
    onSelectOption: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Таймер сна") },
        text = {
            Column {
                if (currentTimerMinutes != null) {
                    Text(
                        "Осталось: $currentTimerMinutes мин.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                options.forEach { minutes ->
                    TextButton(
                        onClick = { onSelectOption(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Выключить через $minutes минут")
                    }
                }
            }
        },
        confirmButton = {
            if (currentTimerMinutes != null) {
                TextButton(onClick = onCancelTimer) {
                    Text("Сбросить таймер", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}