package com.muhil.music.domain.search

import com.muhil.music.domain.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MusicSearchRankerTest {

    private lateinit var ranker: MusicSearchRanker

    @Before
    fun setUp() {
        ranker = MusicSearchRanker()
    }

    private fun createSong(
        id: Long,
        title: String,
        artist: String,
        album: String = "",
        provider: String = Song.PROVIDER_AUDIUS,
        isPreview: Boolean = false
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = 240000L,
            albumArtUri = "https://art.muhil/$id.jpg",
            uri = "https://stream.muhil/$id.mp3",
            isOnline = true,
            provider = provider,
            providerTrackId = id.toString(),
            isPreview = isPreview
        )
    }

    @Test
    fun exactSongTitleMatch_ranksFirst() {
        val target = createSong(1, "Arabic Kuthu", "Anirudh Ravichander", "Beast (Original Motion Picture Soundtrack)")
        val similar = createSong(2, "Arabic Kuthu - Lofi Remix", "Remix Artist", "Chill Beats")
        val unrelated = createSong(3, "Kuthu Fire", "Various", "Tamil Dance Hits")

        val result = ranker.rankSearchResults(
            rawQuery = "Arabic Kuthu",
            songs = listOf(unrelated, similar, target)
        )

        assertNotNull(result.topMatch)
        assertEquals(target.id, result.topMatch?.id)
        assertEquals(target.id, result.rankedSongs.first().id)
    }

    @Test
    fun exactTitleAndArtistMatch_outranksPartialTitle() {
        val songExactBoth = createSong(1, "Naa Ready", "Thalapathy Vijay & Anirudh", "Leo")
        val songCover = createSong(2, "Naa Ready Cover", "Random Singer", "Cover Album")
        val songInstrumental = createSong(3, "Naa Ready (Karaoke Version)", "Karaoke Team", "Karaoke Tracks")

        val result = ranker.rankSearchResults(
            rawQuery = "Naa Ready Anirudh",
            songs = listOf(songCover, songInstrumental, songExactBoth)
        )

        assertEquals(songExactBoth.id, result.topMatch?.id)
        assertEquals(songExactBoth.id, result.rankedSongs.first().id)
    }

    @Test
    fun movieSoundtrackQuery_groupsMovieTracksAndRanksMovieTracksHigh() {
        val track1 = createSong(1, "Naa Ready", "Thalapathy Vijay", "Leo")
        val track2 = createSong(2, "Badass", "Anirudh Ravichander", "Leo")
        val track3 = createSong(3, "Ordinary Person", "Nikhita Gandhi", "Leo")
        val unrelated = createSong(4, "Leo Das Theme Remix", "DJ Beats", "Single")

        val result = ranker.rankSearchResults(
            rawQuery = "Leo",
            songs = listOf(unrelated, track1, track2, track3)
        )

        assertEquals(QueryIntent.MOVIE, result.intent)
        assertNotNull(result.movieSoundtrack)
        assertEquals("Leo", result.movieSoundtrack?.movieName)
        assertTrue(result.movieSoundtrack?.tracks?.size ?: 0 >= 3)
    }

    @Test
    fun tamilScriptSearch_resolvesTransliterationCorrectly() {
        val vikramTrack = createSong(1, "Porkanda Singam", "Anirudh Ravichander", "Vikram")
        val otherTrack = createSong(2, "Shape of You", "Ed Sheeran", "Divide")

        // Search for "விக்ரம்" (Tamil script for Vikram)
        val result = ranker.rankSearchResults(
            rawQuery = "விக்ரம்",
            songs = listOf(otherTrack, vikramTrack)
        )

        assertEquals(QueryIntent.MOVIE, result.intent)
        assertEquals(vikramTrack.id, result.rankedSongs.first().id)
    }

    @Test
    fun minorSpellingMistakes_fuzzyMatchingFindsTarget() {
        val song = createSong(1, "Halamithi Habibo", "Anirudh Ravichander", "Beast")
        val other = createSong(2, "Something Completely Different", "Someone Else", "Album")

        // User typed "Halamithi Habebo" (1 letter typo)
        val result = ranker.rankSearchResults(
            rawQuery = "Halamithi Habebo",
            songs = listOf(other, song)
        )

        assertEquals(song.id, result.topMatch?.id)
    }

    @Test
    fun deduplication_favorsFullTrackOverPreview() {
        val preview = createSong(1, "Chilla Chilla", "Anirudh", "Thunivu", provider = Song.PROVIDER_ITUNES, isPreview = true)
        val fullTrack = createSong(2, "Chilla Chilla", "Anirudh", "Thunivu", provider = Song.PROVIDER_AUDIUS, isPreview = false)

        val result = ranker.rankSearchResults(
            rawQuery = "Chilla Chilla",
            songs = listOf(preview, fullTrack)
        )

        val matchingSongs = result.rankedSongs.filter { it.title.equals("Chilla Chilla", ignoreCase = true) }
        assertEquals(1, matchingSongs.size)
        assertFalse("Must keep full track over preview", matchingSongs.first().isPreview)
    }

    @Test
    fun karaokeDeprioritization_unrequestedKaraokePushedDown() {
        val original = createSong(1, "Kanmani Anbodu", "Kamal Haasan", "Gunaa")
        val karaoke = createSong(2, "Kanmani Anbodu (Karaoke)", "Karaoke Team", "Karaoke Album")

        val result = ranker.rankSearchResults(
            rawQuery = "Kanmani Anbodu",
            songs = listOf(karaoke, original)
        )

        assertEquals(original.id, result.rankedSongs.first().id)
        assertTrue(result.rankedSongs.indexOfFirst { it.id == original.id } < result.rankedSongs.indexOfFirst { it.id == karaoke.id })
    }

    @Test
    fun preferredLanguageRanking_givesTamilSongsHigherRankWhenTamilPreferred() {
        val hindiSong = createSong(1, "Tum Hi Ho", "Arijit Singh", "Aashiqui 2")
        val tamilSong = createSong(2, "Enna Sona - Tamil Version", "A.R. Rahman", "OK Jaanu")

        val result = ranker.rankSearchResults(
            rawQuery = "Enna Sona",
            songs = listOf(hindiSong, tamilSong),
            preferredLanguage = "Tamil"
        )

        assertEquals(tamilSong.id, result.rankedSongs.first().id)
    }

    @Test
    fun emptyAndMalformedQueries_handledSafelyWithoutCrashing() {
        val song = createSong(1, "Test Song", "Test Artist", "Test Album")

        val emptyResult = ranker.rankSearchResults("", listOf(song))
        assertTrue(emptyResult.rankedSongs.isEmpty())

        val whitespaceResult = ranker.rankSearchResults("   ", listOf(song))
        assertTrue(whitespaceResult.rankedSongs.isEmpty())

        val symbolResult = ranker.rankSearchResults("???***!!!", listOf(song))
        assertNotNull(symbolResult)
    }

    @Test
    fun intentDetection_accuratelyClassifiesQueries() {
        assertEquals(QueryIntent.MOVIE, ranker.detectIntent("Jailer"))
        assertEquals(QueryIntent.MOVIE, ranker.detectIntent("Leo Soundtrack"))
        assertEquals(QueryIntent.ARTIST, ranker.detectIntent("Anirudh Ravichander"))
        assertEquals(QueryIntent.ARTIST, ranker.detectIntent("A.R. Rahman"))
        assertEquals(QueryIntent.SONG, ranker.detectIntent("Arabic Kuthu Song"))
    }
}
