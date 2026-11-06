package com.example.mazmazradio.data

/**
 * Стабильный ключ станции для долговременного хранения: избранного и последней
 * включённой станции.
 *
 * URL в плейлисте правятся руками, и один и тот же эфир уже не раз переезжал на
 * другой mount. Ключ не зависит от схемы (http/https), query (`?token=…`), fragment,
 * завершающего слэша и порта по умолчанию, поэтому косметическая правка плейлиста
 * не обнуляет сохранённое.
 */
object StationKey {

    fun of(streamUrl: String): String {
        var u = streamUrl.trim()
        val schemeIdx = u.indexOf("://")
        if (schemeIdx >= 0) u = u.substring(schemeIdx + 3)
        u = u.substringBefore('#').substringBefore('?').trimEnd('/')

        val slash = u.indexOf('/')
        val authority = if (slash == -1) u else u.substring(0, slash)
        val path = if (slash == -1) "" else u.substring(slash)

        val hostPort = authority.substringAfterLast('@')
        val port = hostPort.substringAfterLast(':', "")
        val keepPort = port.isNotEmpty() && port != "80" && port != "443"
        val normalizedAuthority =
            if (keepPort) hostPort else hostPort.substringBeforeLast(':')

        return (normalizedAuthority + path).lowercase()
    }
}
