package com.muhil.music.data.remote

import com.muhil.music.domain.model.LyricsState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OnlineLyricsDataSourceTest {

    private lateinit var lyricsDataSource: OnlineLyricsDataSource

    @Before
    fun setUp() {
        lyricsDataSource = OnlineLyricsDataSource()
    }

    @Test
    fun parseLrc_extractsTimestampedLines() {
        val lrc = """
            [00:12.50]First line of the song
            [00:18.25]Second line arrives
            [01:05.10]Chorus kicks in
        """.trimIndent()

        val lines = lyricsDataSource.parseLrc(lrc)
        assertEquals(3, lines.size)

        assertEquals(12500L, lines[0].timestampMs)
        assertEquals("First line of the song", lines[0].text)

        assertEquals(18250L, lines[1].timestampMs)
        assertEquals("Second line arrives", lines[1].text)

        assertEquals(65100L, lines[2].timestampMs)
        assertEquals("Chorus kicks in", lines[2].text)
    }

    @Test
    fun sanitize_removesNoiseTags() {
        val dirty = "In The End (Remastered 2020) [Feat. Chester Bennington] - Deluxe Edition"
        val clean = lyricsDataSource.sanitize(dirty)
        assertEquals("In The End", clean)
    }
}
