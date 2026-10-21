package com.muhil.music.domain.recommendation

import com.muhil.music.domain.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MusicRecommendationEngineTest {

    private lateinit var engine: MusicRecommendationEngine

    @Before
    fun setUp() {
        engine = MusicRecommendationEngine()
    }

    private fun createSong(
        id: Long,
        title: String,
        artist: String,
        album: String = "",
        genre: String = "",
        provider: String = Song.PROVIDER_AUDIUS,
        isPreview: Boolean = false,
        uri: String = "https://stream.muhil/$id"
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = if (genre.isNotBlank()) "$album $genre" else album,
            duration = 200000L,
            albumArtUri = "https://art.muhil/$id.jpg",
            uri = uri,
            isOnline = true,
            provider = provider,
            providerTrackId = id.toString(),
            isPreview = isPreview
        )
    }

    @Test
    fun seedExclusion_seedSongNeverAppearsInRecommendations() {
        val seed = createSong(1, "Arabic Kuthu", "Anirudh Ravichander", "Beast", "Tamil")
        val candidateSame = createSong(101, "Arabic Kuthu", "Anirudh Ravichander", "Beast", "Tamil")
        val candidateDifferent = createSong(2, "Halamithi Habibo", "Anirudh Ravichander", "Beast", "Tamil")

        val recommendations = engine.rankRecommendations(
            seed = seed,
            candidates = listOf(candidateSame, candidateDifferent)
        )

        assertEquals(1, recommendations.size)
        assertEquals(candidateDifferent.id, recommendations[0].id)
    }

    @Test
    fun crossProviderDeduplication_keepsSingleBestCandidate() {
        val seed = createSong(1, "Shape of You", "Ed Sheeran", "Divide", "Pop")
        val candidateItunes = createSong(2, "Perfect", "Ed Sheeran", "Divide", "Pop", provider = Song.PROVIDER_ITUNES, isPreview = true)
        val candidateAudius = createSong(3, "Perfect", "Ed Sheeran", "Divide", "Pop", provider = Song.PROVIDER_AUDIUS, isPreview = false)
        val candidateOther = createSong(4, "Castle on the Hill", "Ed Sheeran", "Divide", "Pop")

        val recommendations = engine.rankRecommendations(
            seed = seed,
            candidates = listOf(candidateItunes, candidateAudius, candidateOther)
        )

        val perfectCount = recommendations.count { it.title.equals("Perfect", ignoreCase = true) }
        assertEquals(1, perfectCount)
    }

    @Test
    fun artistDiversityCapping_limitsToMaxTwoSongsPerArtist() {
        val seed = createSong(1, "Bad Eyes", "The Weeknd", "After Hours", "R&B")
        val weekndSongs = (2..10).map { i ->
            createSong(i.toLong(), "Weeknd Track $i", "The Weeknd", "After Hours", "R&B")
        }
        val otherArtistsSongs = (20..35).map { i ->
            createSong(i.toLong(), "Other Track $i", "Artist $i", "Album $i", "R&B")
        }

        val recommendations = engine.rankRecommendations(
            seed = seed,
            candidates = weekndSongs + otherArtistsSongs,
            limit = 10
        )

        val weekndCount = recommendations.count { it.artist.equals("The Weeknd", ignoreCase = true) }
        assertEquals(MusicRecommendationEngine.MAX_TRACKS_PER_ARTIST, weekndCount)
        assertEquals(10, recommendations.size)
    }

    @Test
    fun scoring_tamilAffinityBoostsTamilSeedAndCandidate() {
        val seed = createSong(1, "Vathi Coming", "Anirudh Ravichander", "Master", "Tamil")
        val candidateTamil = createSong(2, "Why This Kolaveri Di", "Dhanush", "3", "Tamil")
        val candidateWestern = createSong(3, "Dance Monkey", "Tones and I", "The Kids Are Coming", "Pop")

        val scoreTamil = engine.scoreCandidate(seed, candidateTamil, UserPreferenceProfile())
        val scoreWestern = engine.scoreCandidate(seed, candidateWestern, UserPreferenceProfile())

        assertTrue(
            "Tamil affinity score (${scoreTamil.score}) should exceed Western pop score (${scoreWestern.score})",
            scoreTamil.score > scoreWestern.score
        )
        assertTrue(scoreTamil.reasons.contains("Tamil Music Affinity"))
    }

    @Test
    fun scoring_bollywoodAffinityBoostsBollywoodSeedAndCandidate() {
        val seed = createSong(1, "Tum Hi Ho", "Arijit Singh", "Aashiqui 2", "Bollywood")
        val candidateBolly = createSong(2, "Channa Mereya", "Pritam", "Ae Dil Hai Mushkil", "Bollywood")
        val candidateElectro = createSong(3, "Strobe", "Deadmau5", "For Lack of a Better Name", "Electronic")

        val scoreBolly = engine.scoreCandidate(seed, candidateBolly, UserPreferenceProfile())
        val scoreElectro = engine.scoreCandidate(seed, candidateElectro, UserPreferenceProfile())

        assertTrue(scoreBolly.score > scoreElectro.score)
        assertTrue(scoreBolly.reasons.contains("Bollywood Music Affinity"))
    }

    @Test
    fun scoring_exactGenreBeatsCompatibleGenreBeatsUnrelated() {
        val seed = createSong(1, "Levels", "Avicii", "Electronic Hit", "Electronic")
        val candidateExact = createSong(2, "Titanium", "David Guetta", "Electronic Hit", "Electronic")
        val candidateCompatible = createSong(3, "One More Time", "Daft Punk", "Dance Floor", "Dance")
        val candidateUnrelated = createSong(4, "Sweet Child O Mine", "Guns N Roses", "Appetite", "Rock")

        val scoreExact = engine.scoreCandidate(seed, candidateExact, UserPreferenceProfile())
        val scoreCompatible = engine.scoreCandidate(seed, candidateCompatible, UserPreferenceProfile())
        val scoreUnrelated = engine.scoreCandidate(seed, candidateUnrelated, UserPreferenceProfile())

        assertTrue(scoreExact.score > scoreCompatible.score)
        assertTrue(scoreCompatible.score > scoreUnrelated.score)
    }

    @Test
    fun userBehavior_favoriteAndReplaySignalsIncreaseScore() {
        val seed = createSong(1, "Animals", "Martin Garrix", "Electronic Hit", "Electronic")
        val candidateNormal = createSong(2, "Tremor", "Dimitri Vegas", "Electronic Hit", "Electronic")
        val candidateFavAndReplayed = createSong(3, "Tsunami", "DVBBS", "Electronic Hit", "Electronic")

        val profile = UserPreferenceProfile(
            favoriteSongIds = setOf(3L),
            playCounts = mapOf(3L to 5)
        )

        val scoreNormal = engine.scoreCandidate(seed, candidateNormal, profile)
        val scoreFav = engine.scoreCandidate(seed, candidateFavAndReplayed, profile)

        assertTrue(scoreFav.score > scoreNormal.score)
        assertTrue(scoreFav.reasons.contains("User Favorite"))
        assertTrue(scoreFav.reasons.any { it.contains("Frequently Replayed") })
    }

    @Test
    fun userBehavior_skippedAndRecentlyPlayedArePenalized() {
        val seed = createSong(1, "Animals", "Martin Garrix", "Electronic Hit", "Electronic")
        val candidateNormal = createSong(2, "Tremor", "Dimitri Vegas", "Electronic Hit", "Electronic")
        val candidateSkipped = createSong(3, "Tsunami", "DVBBS", "Electronic Hit", "Electronic")
        val candidateRecent = createSong(4, "Wizard", "Jay Hardway", "Electronic Hit", "Electronic")

        val profile = UserPreferenceProfile(
            skippedSongIds = setOf(3L),
            recentSongIds = listOf(4L, 99L)
        )

        val scoreNormal = engine.scoreCandidate(seed, candidateNormal, profile)
        val scoreSkipped = engine.scoreCandidate(seed, candidateSkipped, profile)
        val scoreRecent = engine.scoreCandidate(seed, candidateRecent, profile)

        assertTrue(scoreNormal.score > scoreSkipped.score)
        assertTrue(scoreNormal.score > scoreRecent.score)
        assertTrue(scoreSkipped.reasons.contains("Previously Skipped"))
        assertTrue(scoreRecent.reasons.contains("Just Played Recently"))
    }

    @Test
    fun providerQualitySignal_fullLengthAudiusPrioritizedOverPreview() {
        val seed = createSong(1, "Closer", "The Chainsmokers", "Collage", "Pop")
        val candidatePreview = createSong(2, "Roses", "The Chainsmokers", "Roses EP", "Pop", provider = Song.PROVIDER_ITUNES, isPreview = true)
        val candidateFullTrack = createSong(3, "Don't Let Me Down", "The Chainsmokers", "Collage", "Pop", provider = Song.PROVIDER_AUDIUS, isPreview = false)

        val scorePreview = engine.scoreCandidate(seed, candidatePreview, UserPreferenceProfile())
        val scoreFullTrack = engine.scoreCandidate(seed, candidateFullTrack, UserPreferenceProfile())

        assertTrue(scoreFullTrack.score > scorePreview.score)
        assertTrue(scoreFullTrack.reasons.contains("Authorized Full Track"))
    }

    @Test
    fun rankRecommendations_skipsEmptyUriCandidates() {
        val seed = createSong(1, "Closer", "The Chainsmokers", "Collage", "Pop")
        val candidateInvalid = createSong(2, "Invalid Track", "The Chainsmokers", "Collage", "Pop", uri = "")
        val candidateValid = createSong(3, "Valid Track", "The Chainsmokers", "Collage", "Pop", uri = "https://stream.muhil/3")

        val recommendations = engine.rankRecommendations(seed, listOf(candidateInvalid, candidateValid))
        assertEquals(1, recommendations.size)
        assertEquals(candidateValid.id, recommendations[0].id)
    }
}
