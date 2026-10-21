package com.muhil.music.domain.favorites

import com.muhil.music.domain.model.Song
import org.junit.Assert.*
import org.junit.Test

class FavoritesLogicTest {

    private fun createSong(id: Long, title: String, artist: String): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = "Test Album",
            duration = 180000L,
            albumArtUri = "https://art.muhil/$id.jpg",
            uri = "https://stream.muhil/$id",
            isOnline = true,
            provider = Song.PROVIDER_SAAVN,
            providerTrackId = id.toString(),
            isPreview = false
        )
    }

    @Test
    fun optimisticAdd_updatesIdsAndPrependsSong() {
        val song1 = createSong(1, "Arabic Kuthu", "Anirudh")
        val currentIds = emptySet<Long>()
        val currentSongs = emptyList<Song>()

        val newIds = currentIds + song1.id
        val newSongs = listOf(song1) + currentSongs.filter { it.id != song1.id }

        assertTrue(newIds.contains(1L))
        assertEquals(1, newSongs.size)
        assertEquals(song1.id, newSongs[0].id)
    }

    @Test
    fun optimisticRemove_removesIdAndFiltersFromList() {
        val song1 = createSong(1, "Arabic Kuthu", "Anirudh")
        val song2 = createSong(2, "Master Coming", "Anirudh")
        val currentIds = setOf(1L, 2L)
        val currentSongs = listOf(song1, song2)

        val newIds = currentIds - song1.id
        val newSongs = currentSongs.filter { it.id != song1.id }

        assertFalse(newIds.contains(1L))
        assertTrue(newIds.contains(2L))
        assertEquals(1, newSongs.size)
        assertEquals(song2.id, newSongs[0].id)
    }

    @Test
    fun resolveSong_findsTrackAcrossMultipleCollections() {
        val songNowPlaying = createSong(10, "Now Playing Track", "Artist A")
        val songMelody = createSong(20, "Melody Track", "Artist B")
        val songSoundtrack = createSong(30, "Soundtrack Track", "Artist C")
        val songQueue = createSong(40, "Queue Track", "Artist D")

        val tamilMelodies = listOf(songMelody)
        val soundtrackTracks = listOf(songSoundtrack)
        val queue = listOf(songQueue)

        fun resolve(id: Long): Song? {
            if (songNowPlaying.id == id) return songNowPlaying
            return tamilMelodies.find { it.id == id }
                ?: soundtrackTracks.find { it.id == id }
                ?: queue.find { it.id == id }
        }

        assertEquals("Now Playing Track", resolve(10L)?.title)
        assertEquals("Melody Track", resolve(20L)?.title)
        assertEquals("Soundtrack Track", resolve(30L)?.title)
        assertEquals("Queue Track", resolve(40L)?.title)
        assertNull(resolve(999L))
    }
}
