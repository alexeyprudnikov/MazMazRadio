package com.example.mazmazradio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mazmazradio.R

/**
 * Значок приложения — тот же мотив, что у иконки лаунчера (см. drawable/ic_app_logo.xml).
 * Используется в заголовке TopAppBar и в шапке полноэкранного плеера.
 *
 * [tint] != null — однотонная версия мотива ([drawable/ic_app_mark.xml]) без фирменного
 * фона: для плейсхолдеров и других мест, где цветной логотип перетягивает внимание.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier.size(30.dp),
    shape: Shape = CircleShape,
    contentScale: ContentScale = ContentScale.Fit,
    tint: Color? = null
) {
    Image(
        painter = painterResource(
            if (tint == null) R.drawable.ic_app_logo else R.drawable.ic_app_mark
        ),
        contentDescription = null,
        contentScale = contentScale,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier.clip(shape)
    )
}
