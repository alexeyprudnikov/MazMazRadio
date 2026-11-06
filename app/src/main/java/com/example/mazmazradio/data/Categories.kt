package com.example.mazmazradio.data

/**
 * Категории плейлиста. Значения — это и group-title в m3u, и заголовки аккордеонов в UI,
 * поэтому строки должны совпадать с [assets/playlists/hostingradio.m3u] буквально.
 *
 * [ORDER] задаёт порядок групп в списке (плюс [rank] для сортировки).
 */
object Categories {
    const val POP = "Поп и главные эфиры"
    const val ROCK = "Рок и альтернатива"
    const val DOROGNOE = "Дорожное радио · Тематические"
    const val DANCE = "DFM · Танцы и клуб"
    const val JAZZ = "Джаз, классика и релакс"
    const val TALK = "Разговорное и новости"
    const val CHANSON = "Шансон"

    /** Категория для потоков без group-title. */
    const val DEFAULT = "Разное"

    /** Служебная группа избранного, формируется в ViewModel. */
    const val FAVORITES = "Избранное"

    // Юмор/спорт/детям и Санкт-Петербург были в плейлисте, но все их потоки
    // оказались мёртвыми — категории убраны вместе со станциями.
    val ORDER = listOf(POP, ROCK, DOROGNOE, DANCE, JAZZ, TALK, CHANSON)

    /** Порядок группы: известные категории по [ORDER], остальные — в конце по алфавиту. */
    fun rank(name: String): Int = ORDER.indexOf(name).let { if (it == -1) ORDER.size else it }
}
