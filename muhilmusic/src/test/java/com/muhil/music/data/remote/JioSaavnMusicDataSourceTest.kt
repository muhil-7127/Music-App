package com.muhil.music.data.remote

import com.muhil.music.domain.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JioSaavnMusicDataSourceTest {

    private lateinit var dataSource: JioSaavnMusicDataSource

    @Before
    fun setUp() {
        dataSource = JioSaavnMusicDataSource()
    }

    @Test
    fun decryptMediaUrl_decryptsValidDESMediaUrlAndUpgradesBitrate() {
        val encrypted = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyPgia5m7T7U4VLl0JRX9Gn4HEX+RKmDEyEYR6Up7jk21qN7/qcIWm7Bw7tS9a8Gtq"
        val decrypted = dataSource.decryptMediaUrl(encrypted)

        assertNotNull("Decrypted URL must not be null", decrypted)
        assertTrue("URL must be an mp4/aac audio stream", decrypted!!.endsWith(".mp4") || decrypted.endsWith(".m4a"))
        assertTrue("URL must be upgraded to 160kbps high quality", decrypted.endsWith("_160.mp4") || decrypted.endsWith("_160.m4a"))
        assertTrue("URL must point to Saavn CDN", decrypted.startsWith("https://") || decrypted.startsWith("http://"))
    }

    @Test
    fun decryptMediaUrl_returnsNullForBlankOrCorruptedInput() {
        assertNull(dataSource.decryptMediaUrl(""))
        assertNull(dataSource.decryptMediaUrl("   "))
        assertNull(dataSource.decryptMediaUrl("invalid_base64_not_encrypted"))
    }

    @Test
    fun unescapeHtml_decodesCommonHtmlEntities() {
        val raw = "Arabic Kuthu &quot;Halamithi Habibo&quot; &amp; Beast (&#039;Leo&#039; &lt;Tamil&gt;)"
        val unescaped = dataSource.unescapeHtml(raw)
        assertEquals("Arabic Kuthu \"Halamithi Habibo\" & Beast ('Leo' <Tamil>)", unescaped)
    }

    @Test
    fun resolveArtworkUrl_upgradesTo500x500HD() {
        val lowRes = "https://c.saavncdn.com/722/Arabic-Kuthu-150x150.jpg"
        val hd = dataSource.resolveArtworkUrl(lowRes)
        assertEquals("https://c.saavncdn.com/722/Arabic-Kuthu-500x500.jpg", hd)
    }

    @Test
    fun saavnIdToLong_isDeterministicAndPositive() {
        val id1 = dataSource.saavnIdToLong("1234567")
        val id2 = dataSource.saavnIdToLong("1234567")
        val id3 = dataSource.saavnIdToLong("abcdef")

        assertEquals(id1, id2)
        assertTrue("ID must be positive", id1 > 0)
        assertTrue("Hashed ID must be positive", id3 > 0)
    }

    @Test
    fun parseSongs_extractsFullLengthPlayableSongs() {
        val json = """
            {
                "results": [
                    {
                        "id": "song_001",
                        "song": "Arabic Kuthu &quot;Halamithi Habibo&quot;",
                        "singers": "Anirudh Ravichander, Jonita Gandhi",
                        "album": "Beast",
                        "duration": "280",
                        "image": "https://c.saavncdn.com/150x150.jpg",
                        "encrypted_media_url": "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyPgia5m7T7U4VLl0JRX9Gn4HEX+RKmDEyEYR6Up7jk21qN7/qcIWm7Bw7tS9a8Gtq"
                    }
                ]
            }
        """.trimIndent()

        val songs = dataSource.parseSongs(json)
        assertEquals(1, songs.size)

        val song = songs[0]
        assertEquals("Arabic Kuthu \"Halamithi Habibo\"", song.title)
        assertEquals("Anirudh Ravichander, Jonita Gandhi", song.artist)
        assertEquals("Beast", song.album)
        assertEquals(280000L, song.duration)
        assertTrue("Must be marked as online song", song.isOnline)
        assertFalse("Must NOT be a 30s preview - 100% full song", song.isPreview)
        assertEquals(Song.PROVIDER_SAAVN, song.provider)
        assertEquals("https://c.saavncdn.com/500x500.jpg", song.albumArtUri)
        assertTrue("URI must be high quality audio stream", song.uri.endsWith("_160.mp4") || song.uri.endsWith("_160.m4a"))
    }

    @Test
    fun parseAlbums_extractsAlbumListCorrectly() {
        val json = """
            {
                "results": [
                    {
                        "id": "album_99",
                        "title": "Leo &quot;Original Soundtrack&quot;",
                        "artist": "Anirudh Ravichander",
                        "image": "https://c.saavncdn.com/150x150.jpg",
                        "year": "2023",
                        "song_count": "7"
                    }
                ]
            }
        """.trimIndent()

        val albums = dataSource.parseAlbums(json)
        assertEquals(1, albums.size)

        val album = albums[0]
        assertEquals("Leo \"Original Soundtrack\"", album.title)
        assertEquals("Anirudh Ravichander", album.artist)
        assertEquals(7, album.songCount)
        assertEquals(2023, album.year)
        assertEquals(Song.PROVIDER_SAAVN, album.provider)
        assertEquals("https://c.saavncdn.com/500x500.jpg", album.artUri)
    }
}
