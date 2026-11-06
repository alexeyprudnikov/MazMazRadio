package com.example.mazmazradio

import com.example.mazmazradio.data.Categories
import com.example.mazmazradio.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Проверяет, что источник (asset-плейлист) и категории согласованы: парсер читает
 * все потоки, у каждого есть название и известная категория, мёртвых потоков нет.
 * Плейлист читается напрямую из assets — контекст Android парсеру для этого не нужен.
 */
class PlaylistTest {

    private val playlistFile: File
        get() {
            val candidates = listOf(
                "src/main/assets/playlists/hostingradio.m3u",
                "app/src/main/assets/playlists/hostingradio.m3u"
            )
            return candidates.map(::File).firstOrNull { it.isFile }
                ?: throw IllegalStateException("Не найден плейлист: $candidates")
        }

    private val stations by lazy {
        M3uParser().parseContent(playlistFile.readText())
    }

    @Test
    fun `playlist parses every station`() {
        val extinfCount = playlistFile.readLines().count { it.startsWith("#EXTINF:") }
        assertEquals(extinfCount, stations.size)
        assertTrue("плейлист пуст", stations.isNotEmpty())
        assertTrue(
            "дубли потоков: " + stations.groupBy { it.streamUrl }.filterValues { it.size > 1 }.keys,
            stations.map { it.streamUrl }.distinct().size == stations.size
        )
        stations.forEach {
            assertTrue("пустое название у ${it.streamUrl}", it.title.isNotBlank())
            assertTrue("название с мусором из атрибутов: ${it.title}", !it.title.contains("group-title"))
            assertTrue("неизвестная категория '${it.category}' у ${it.title}", it.category in Categories.ORDER)
        }
    }

    @Test
    fun `categories cover the playlist`() {
        val counts = stations.groupingBy { it.category }.eachCount()
        assertEquals("поп-станций", 9, counts[Categories.POP])
        assertEquals("рока", 5, counts[Categories.ROCK])
        assertEquals("дорожных тематических", 3, counts[Categories.DOROGNOE])
        assertEquals("танцев DFM", 5, counts[Categories.DANCE])
        assertEquals("джаза и классики", 5, counts[Categories.JAZZ])
        assertEquals("разговорных", 4, counts[Categories.TALK])
        assertEquals("шансона", 1, counts[Categories.CHANSON])
        // пустых аккордеонов быть не должно: каждая категория из ORDER чем-то закрыта
        Categories.ORDER.forEach {
            assertTrue("категория '$it' пуста", (counts[it] ?: 0) > 0)
        }
        assertEquals("всего живых потоков", Categories.ORDER.sumOf { counts[it] ?: 0 }, stations.size)
    }

    @Test
    fun `known streams keep their title and category`() {
        fun stationOf(url: String) = stations.firstOrNull { it.streamUrl == url }

        assertEquals(Categories.POP, stationOf("http://ep128.hostingradio.ru:8030/ep128")?.category)
        assertEquals("Европа Плюс (Москва 128k)", stationOf("http://ep128.hostingradio.ru:8030/ep128")?.title)
        assertEquals(Categories.POP, stationOf("https://dfm.hostingradio.ru/dfm128.mp3")?.category)
        assertEquals(Categories.ROCK, stationOf("https://nashe1.hostingradio.ru/nashe-128.mp3")?.category)
        assertEquals(Categories.DOROGNOE, stationOf("https://hls-01-dorognoe-rock.emgsound.ru/18/playlist.m3u8")?.category)
        assertEquals(Categories.DANCE, stationOf("https://hls-01-dfm.hostingradio.ru/dfmclub/playlist.m3u8")?.category)
        assertEquals(Categories.JAZZ, stationOf("http://orfeyfm.hostingradio.ru:8034/orfeyfm128.mp3")?.category)
        assertEquals(Categories.TALK, stationOf("https://solovievfm.hostingradio.ru/solovievfm128.aacp")?.category)
        assertEquals(Categories.CHANSON, stationOf("https://chanson.hostingradio.ru:8041/chanson128.mp3")?.category)

        assertNotNull(
            "категории в плейлисте и в Categories.ORDER разошлись по написанию",
            stationOf("https://chanson.hostingradio.ru:8041/chanson128.mp3")
        )
    }

    @Test
    fun `station without group-title falls back to the default category`() {
        val parsed = M3uParser().parseContent(
            """
            #EXTM3U
            #EXTINF:-1,Без группы
            https://example.com/stream.mp3
            """.trimIndent()
        )
        assertEquals(1, parsed.size)
        assertEquals(Categories.DEFAULT, parsed.single().category)
    }

    @Test
    fun `extinf lines are well formed`() {
        // Название идёт сразу после закрывающей кавычки атрибутов; кавычек и лишних
        // запятых в нём быть не должно, иначе сторонний плеер (VLC/mpv) прочитает
        // название с мусором, даже когда наш парсер его переваривает.
        val wellFormed = """#EXTINF:-1 group-title="[^"]*",[^"#][^"]*""".toRegex()
        playlistFile.readLines()
            .filter { it.startsWith("#EXTINF:") }
            .forEach { assertTrue("кривой #EXTINF: $it", wellFormed.matches(it)) }
    }

    @Test
    fun `title survives a comma inside group-title`() {
        val parsed = M3uParser().parseContent(
            """
            #EXTM3U
            #EXTINF:-1 group-title="Джаз, классика и релакс",Радио ВЕРА
            https://example.com/stream.mp3
            """.trimIndent()
        )
        assertEquals("Радио ВЕРА", parsed.single().title)
        assertEquals("Джаз, классика и релакс", parsed.single().category)
    }

    @Test
    fun `playlist has no dead streams left`() {
        // Остальные снятые потоки с причинами — в docs/dead-streams.md.
        val dead = listOf(
            "https://nashe1.hostingradio.ru/rock90-128.mp3",
            "https://nashe1.hostingradio.ru/punk-128.mp3",
            "https://nashe1.hostingradio.ru/heavy-128.mp3",
            "https://nashe1.hostingradio.ru/legends-128.mp3",
            "https://nashe1.hostingradio.ru/rusvolna-128.mp3",
            "https://nashe1.hostingradio.ru/web-128.mp3",
            "https://nashe1.hostingradio.ru/smoothjazz-128.mp3",
            "https://nashe1.hostingradio.ru/classicjazz-128.mp3",
            "https://nashe1.hostingradio.ru/blues-128.mp3",
            "https://dfm.hostingradio.ru/dfmtop40128.mp3",
            "https://emgspb.hostingradio.ru/newradio128.aacp",
            "https://mayak.hostingradio.ru/mayak128.aacp",
            "https://vestifm.hostingradio.ru/vestifm128.aacp",
            "https://radiorossii.hostingradio.ru/radiorossii128.aacp",
            "https://govoritmoskva.hostingradio.ru/gm128.aacp",
            "https://cultura.hostingradio.ru/cultura128.aacp",
            "https://comedy.hostingradio.ru/comedy128.aacp",
            "https://deti.hostingradio.ru/deti128.aacp",
            "https://piterfm.hostingradio.ru/piterfm128.aacp",
            "https://modern.hostingradio.ru/modern128.aacp",
            "https://chanson.hostingradio.ru/romance128.mp3"
        )
        val present = stations.map { it.streamUrl }.toSet()
        val offenders = dead.filter { it in present }
        assertTrue("в плейлисте остались мёртвые потоки: $offenders", offenders.isEmpty())
    }
}
