package com.muhil.music.data.remote

import com.muhil.music.domain.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudiusMusicDataSourceTest {

    private lateinit var dataSource: AudiusMusicDataSource

    @Before
    fun setUp() {
        dataSource = AudiusMusicDataSource()
    }

    @Test
    fun audiusIdToLong_isDeterministicAndUnique() {
        val id1 = dataSource.audiusIdToLong("V6ddGxW")
        val id2 = dataSource.audiusIdToLong("V6ddGxW")
        val id3 = dataSource.audiusIdToLong("abkvg")

        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
        assertTrue(id1 > 0)
        assertTrue(id3 > 0)
    }

    @Test
    fun parseTracks_extractsFullLengthStreamableTrackCorrectly() {
        val json = """
            {
                "data": [
                    {
                        "id": "track_123",
                        "title": "Cosmic Voyage",
                        "duration": 215,
                        "genre": "Electronic",
                        "is_streamable": true,
                        "is_delete": false,
                        "user": {
                            "name": "Aura DJ"
                        },
                        "artwork": {
                            "150x150": "https://audius.co/art_150.jpg",
                            "480x480": "https://audius.co/art_480.jpg",
                            "1000x1000": "https://audius.co/art_1000.jpg"
                        }
                    }
                ]
            }
        """.trimIndent()

        val songs = dataSource.parseTracks(json)
        assertEquals(1, songs.size)

        val song = songs[0]
        assertEquals("Cosmic Voyage", song.title)
        assertEquals("Aura DJ", song.artist)
        assertEquals(215000L, song.duration) // 215 seconds converted to ms
        assertTrue(song.isOnline)
        assertFalse(song.isPreview) // Full length authorized track!
        assertEquals(Song.PROVIDER_AUDIUS, song.provider)
        assertEquals("track_123", song.providerTrackId)
        assertEquals("https://audius.co/art_480.jpg", song.albumArtUri)
        assertEquals("https://api.audius.co/v1/tracks/track_123/stream?app_name=MuhilMusic", song.uri)
    }

    @Test
    fun parseTracks_filtersOutNonStreamableAndDeletedTracks() {
        val json = """
            {
                "data": [
                    {
                        "id": "gated_1",
                        "title": "Gated Track",
                        "is_streamable": false,
                        "is_delete": false,
                        "user": { "name": "Artist 1" }
                    },
                    {
                        "id": "deleted_1",
                        "title": "Deleted Track",
                        "is_streamable": true,
                        "is_delete": true,
                        "user": { "name": "Artist 2" }
                    },
                    {
                        "id": "valid_1",
                        "title": "Valid Streamable",
                        "is_streamable": true,
                        "is_delete": false,
                        "duration": 180,
                        "user": { "name": "Artist 3" }
                    }
                ]
            }
        """.trimIndent()

        val songs = dataSource.parseTracks(json)
        assertEquals(1, songs.size)
        assertEquals("Valid Streamable", songs[0].title)
    }

    @Test
    fun mapGenre_correctlyMapsCommonGenres() {
        assertEquals("Electronic", dataSource.mapGenre("electronic"))
        assertEquals("Hip-Hop/Rap", dataSource.mapGenre("hip-hop"))
        assertEquals("Pop", dataSource.mapGenre("pop"))
        assertEquals("Rock", dataSource.mapGenre("rock"))
        assertEquals("R&B/Soul", dataSource.mapGenre("r&b"))
        assertEquals("", dataSource.mapGenre("all"))
    }

    @Test
    fun parseUsers_extractsArtistMetadata() {
        val json = """
            {
                "data": [
                    {
                        "id": "user_99",
                        "name": "Skrillex",
                        "track_count": 12,
                        "album_count": 3,
                        "profile_picture": {
                            "480x480": "https://audius.co/skrillex_480.jpg"
                        }
                    }
                ]
            }
        """.trimIndent()

        val artists = dataSource.parseUsers(json)
        assertEquals(1, artists.size)
        val artist = artists[0]
        assertEquals("Skrillex", artist.name)
        assertEquals(12, artist.songCount)
        assertEquals(3, artist.albumCount)
        assertEquals("https://audius.co/skrillex_480.jpg", artist.artUri)
        assertTrue(artist.isOnline)
        assertEquals(Song.PROVIDER_AUDIUS, artist.provider)
        assertEquals("user_99", artist.providerId)
    }

    @Test
    fun parsePlaylists_extractsAlbumMetadata() {
        val json = """
            {
                "data": [
                    {
                        "id": "playlist_77",
                        "playlist_name": "Summer Bass",
                        "track_count": 25,
                        "user": { "name": "Curator" },
                        "artwork": { "480x480": "https://audius.co/summer_480.jpg" }
                    }
                ]
            }
        """.trimIndent()

        val albums = dataSource.parsePlaylists(json)
        assertEquals(1, albums.size)
        val album = albums[0]
        assertEquals("Summer Bass", album.title)
        assertEquals("Curator", album.artist)
        assertEquals(25, album.songCount)
        assertEquals("https://audius.co/summer_480.jpg", album.artUri)
        assertTrue(album.isOnline)
        assertEquals(Song.PROVIDER_AUDIUS, album.provider)
        assertEquals("playlist_77", album.providerId)
    }
}
