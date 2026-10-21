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

    @Test
    fun userBehavior_preferredLanguageBoostsMatchingSongs() {
        val seed = createSong(1, "Shape of You", "Ed Sheeran", "Divide", "Pop")
        val candidateTamil = createSong(2, "Kolaveri", "Dhanush", "3", "Tamil")
        val candidateEnglish = createSong(3, "Galway Girl", "Ed Sheeran", "Divide", "Pop")

        val profileTamil = UserPreferenceProfile(preferredLanguage = "Tamil")
        val profileEnglish = UserPreferenceProfile(preferredLanguage = "English")

        val scoreTamilWithTamilPref = engine.scoreCandidate(seed, candidateTamil, profileTamil)
        val scoreTamilWithEnglishPref = engine.scoreCandidate(seed, candidateTamil, profileEnglish)

        assertTrue(scoreTamilWithTamilPref.score > scoreTamilWithEnglishPref.score)
        assertTrue(scoreTamilWithTamilPref.reasons.contains("Preferred Language Match (Tamil)"))
    }

    @Test
    fun mood_romantic_boostsTamilMelodySongs() {
        val seed = createSong(1, "Generic Track", "Various Artists", "General", "Tamil")
        val candidateMelody = createSong(2, "Vizhiye Kadhale", "Sid Sriram", "Melody Hits", "Tamil")
        val candidateUnrelated = createSong(3, "Heavy Bass EDM", "DJ Electro", "Club 2024", "Electronic")

        val profile = UserPreferenceProfile(currentMood = Mood.ROMANTIC)

        val scoreMelody = engine.scoreCandidate(seed, candidateMelody, profile)
        val scoreUnrelated = engine.scoreCandidate(seed, candidateUnrelated, profile)

        assertTrue(scoreMelody.score > scoreUnrelated.score)
        assertTrue(scoreMelody.reasons.any { it.contains("Romantic") || it.contains("Melody") })
    }

    @Test
    fun mood_energetic_boostsTamilKuthuSongs() {
        val seed = createSong(1, "Generic Track", "Various Artists", "General", "Tamil")
        val candidateKuthu = createSong(2, "Vaathi Raid Kuthu Beat", "Anirudh", "Master", "Tamil")
        val candidateSlow = createSong(3, "Lofi Study Session", "Chill Artist", "Sleep Night", "Indie")

        val profile = UserPreferenceProfile(currentMood = Mood.ENERGETIC)

        val scoreKuthu = engine.scoreCandidate(seed, candidateKuthu, profile)
        val scoreSlow = engine.scoreCandidate(seed, candidateSlow, profile)

        assertTrue(scoreKuthu.score > scoreSlow.score)
        assertTrue(scoreKuthu.reasons.any { it.contains("Energetic") || it.contains("High-Energy") })
    }

    @Test
    fun feedback_dislikedSongIsCompletelyFiltered() {
        val seed = createSong(1, "Seed Track", "Artist", "Album", "Pop")
        val candidateDisliked = createSong(2, "Annoying Song", "Artist", "Album", "Pop")
        val candidateNormal = createSong(3, "Good Song", "Artist", "Album", "Pop")

        val profile = UserPreferenceProfile(dislikedSongIds = setOf(2L))

        val scoreDisliked = engine.scoreCandidate(seed, candidateDisliked, profile)
        val scoreNormal = engine.scoreCandidate(seed, candidateNormal, profile)

        assertEquals(0.0, scoreDisliked.score, 0.001)
        assertTrue(scoreNormal.score > 0)

        val recs = engine.rankRecommendations(seed, listOf(candidateDisliked, candidateNormal), profile)
        assertFalse(recs.any { it.id == 2L })
        assertTrue(recs.any { it.id == 3L })
    }

    @Test
    fun feedback_dislikedArtistIsFiltered() {
        val seed = createSong(1, "Seed Track", "Seed Artist", "Album", "Pop")
        val candidateDislikedArtist = createSong(2, "Song A", "Disliked Singer", "Album", "Pop")
        val candidateNormal = createSong(3, "Song B", "Loved Singer", "Album", "Pop")

        val profile = UserPreferenceProfile(dislikedArtists = setOf("disliked singer"))

        val scoreDisliked = engine.scoreCandidate(seed, candidateDislikedArtist, profile)
        val scoreNormal = engine.scoreCandidate(seed, candidateNormal, profile)

        assertEquals(0.0, scoreDisliked.score, 0.001)
        assertTrue(scoreNormal.score > 0)
    }
}
