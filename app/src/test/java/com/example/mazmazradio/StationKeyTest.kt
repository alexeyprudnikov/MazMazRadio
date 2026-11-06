package com.example.mazmazradio

import com.example.mazmazradio.data.StationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Ключ станции — то, чем измеряется избранное и «последняя станция». Он должен
 * переживать косметическую правку URL в плейлисте и при этом не склеивать разные
 * потоки одного хоста.
 */
class StationKeyTest {

    @Test
    fun `scheme query fragment and trailing slash do not matter`() {
        val key = StationKey.of("https://nashe1.hostingradio.ru/nashe-128.mp3")
        assertEquals(key, StationKey.of("http://nashe1.hostingradio.ru/nashe-128.mp3"))
        assertEquals(key, StationKey.of("https://nashe1.hostingradio.ru/nashe-128.mp3/"))
        assertEquals(key, StationKey.of("https://nashe1.hostingradio.ru/nashe-128.mp3?token=123"))
        assertEquals(key, StationKey.of("https://nashe1.hostingradio.ru/nashe-128.mp3#play"))
        assertEquals(key, StationKey.of("  HTTPS://NASHE1.HostingRadio.ru/NASHE-128.mp3  "))
    }

    @Test
    fun `default ports are dropped, real ports kept`() {
        assertEquals(
            "nashe1.hostingradio.ru/nashe-128.mp3",
            StationKey.of("http://nashe1.hostingradio.ru:80/nashe-128.mp3")
        )
        assertEquals(
            "nashe1.hostingradio.ru/nashe-128.mp3",
            StationKey.of("https://nashe1.hostingradio.ru:443/nashe-128.mp3")
        )
        assertEquals(
            "ep128.hostingradio.ru:8030/ep128",
            StationKey.of("http://ep128.hostingradio.ru:8030/ep128")
        )
    }

    @Test
    fun `different mounts stay different`() {
        assertNotEquals(
            StationKey.of("https://dfm.hostingradio.ru/dfm128.mp3"),
            StationKey.of("https://dfm.hostingradio.ru/hitfm128.mp3")
        )
        assertNotEquals(
            StationKey.of("https://dfm.hostingradio.ru:443/dfm128.mp3"),
            StationKey.of("https://dfm.hostingradio.ru:8443/dfm128.mp3")
        )
    }

    @Test
    fun `hls playlist paths keep their full path`() {
        assertEquals(
            "hls-01-dfm.hostingradio.ru/dfmclub/playlist.m3u8",
            StationKey.of("https://hls-01-dfm.hostingradio.ru/dfmclub/playlist.m3u8")
        )
    }
}
