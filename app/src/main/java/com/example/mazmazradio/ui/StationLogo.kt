package com.example.mazmazradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun StationLogo(
    logoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var isError by remember(logoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNullOrBlank() || isError) {
            FallbackIcon()
        } else {
            val context = LocalContext.current
            val imageRequest = remember(logoUrl) {
                ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .listener(
                        onError = { _, _ ->
                            isError = true
                        },
                        onSuccess = { _, result ->
                            // Если сервер прислал крошечную картинку-заглушку или плашку с ошибкой
                            val width = result.drawable.intrinsicWidth
                            val height = result.drawable.intrinsicHeight
                            if (width < 32 || height < 32) {
                                isError = true
                            }
                        }
                    )
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onError = { isError = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FallbackIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxSize(0.55f)
    )
}