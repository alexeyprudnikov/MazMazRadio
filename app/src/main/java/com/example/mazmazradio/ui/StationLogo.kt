package com.example.mazmazradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * Аватар станции: круглая плашка с нейтральным мотивом приложения.
 *
 * Картинки станций в плейлисте нет (m3u без `tvg-logo`), поэтому здесь всегда
 * плейсхолдер — мотив в приглушённом цвете, чтобы не перетягивал внимание в списке.
 */
@Composable
fun StationLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AppLogo(
            modifier = Modifier.fillMaxSize(0.58f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
