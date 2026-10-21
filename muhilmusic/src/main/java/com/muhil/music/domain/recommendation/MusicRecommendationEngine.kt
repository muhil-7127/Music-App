package com.muhil.music.domain.recommendation

import com.muhil.music.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User listening signals used to personalize song recommendations.
 */
data class UserPreferenceProfile(
    val favoriteSongIds: Set<Long> = emptySet(),
    val favoriteArtists: Set<String> = emptySet(),
    val recentSongIds: List<Long> = emptyList(),
    val skippedSongIds: Set<Long> = emptySet(),
    val playCounts: Map<Long, Int> = emptyMap(),
    val preferredGenres: Set<String> = emptySet(),
    val preferredLanguage: String = "Tamil"
)

/**
 * Scored recommendation candidate with scoring rationale for debugging/explainability.
 */
data class ScoredCandidate(
    val song: Song,
    val score: Double,
    val reasons: List<String>
)

/**
 * Intelligent recommendation engine:
 * Computes multi-signal metadata similarity, user behavior weighting, diversity capping,
 * and cross-provider deduplication without repetition.
 */
@Singleton
class MusicRecommendationEngine @Inject constructor() {

    companion object {
        const val MAX_TRACKS_PER_ARTIST = 2
        private const val SCORE_EXACT_GENRE = 40.0
        private const val SCORE_COMPATIBLE_GENRE = 25.0
        private const val SCORE_SAME_ARTIST = 25.0
        private const val SCORE_FAVORITE_ARTIST = 15.0
        private const val SCORE_FAVORITE_SONG = 15.0
        private const val SCORE_REPLAY_BONUS = 15.0
        private const val SCORE_KEYWORD_MATCH = 20.0
        private const val SCORE_LANGUAGE_AFFINITY = 30.0
        private const val SCORE_FULL_TRACK_BONUS = 12.0
        private const val PENALTY_SKIPPED = -40.0
        private const val PENALTY_RECENTLY_PLAYED = -20.0

        // Genre compatibility matrix
        private val COMPATIBLE_GENRES = mapOf(
            "pop" to setOf("dance", "electronic", "r&b", "indie pop"),
            "electronic" to setOf("dance", "pop", "house", "techno", "edm"),
            "rock" to setOf("alternative", "indie", "metal", "punk"),
            "hip-hop" to setOf("r&b", "rap", "trap"),
            "r&b" to setOf("soul", "hip-hop", "pop"),
            "tamil" to setOf("bollywood", "indian", "kollywood", "melody"),
            "bollywood" to setOf("tamil", "indian", "hindi", "melody"),
            "indie" to setOf("alternative", "folk", "acoustic", "indie pop")
        )

        // Musical style, mood, and language tokens
        private val MOOD_TOKENS = setOf(
            "acoustic", "unplugged", "live", "remix", "lofi", "lo-fi", "chill",
            "slow", "fast", "dance", "club", "piano", "guitar", "bass", "instrumental",
            "ambient", "synth", "party", "summer", "night", "sad", "love", "melody"
        )

        private val TAMIL_SIGNALS = setOf(
            "anirudh", "a.r. rahman", "ar rahman", "yuvan", "harris jayaraj", "ilayaraja",
            "santhosh narayanan", "sid sriram", "dhanush", "gv prakash", "d. imman",
            "kadhal", "kanne", "vizhiye", "oru", "nee", "en", "tamil", "kollywood"
        )

        private val BOLLYWOOD_SIGNALS = setOf(
            "arijit singh", "pritam", "neha kakkar", "badshah", "shreya ghoshal",
            "vishal-shekhar", "amit trivedi", "dil", "pyar", "ishq", "tum", "bollywood", "hindi"
        )
    }

    /**
     * Ranks a collection of candidate songs against a seed song and user preference profile.
     * Enforces artist diversity, deduplication, and exclusion of the seed track itself.
     */
    fun rankRecommendations(
        seed: Song,
        candidates: List<Song>,
        userProfile: UserPreferenceProfile = UserPreferenceProfile(),
        limit: Int = 15
    ): List<Song> {
        val seedNormKey = normalizeKey(seed.title, seed.artist)
        val seenKeys = mutableSetOf<String>()
        seenKeys.add(seedNormKey) // Exclude seed itself

        val scoredList = mutableListOf<ScoredCandidate>()

        for (candidate in candidates) {
            // Must have valid playable stream/preview URI
            if (candidate.uri.isBlank()) continue

            // Deduplicate cross-provider matches
            val candidateNormKey = normalizeKey(candidate.title, candidate.artist)
            if (seenKeys.contains(candidateNormKey)) continue
            seenKeys.add(candidateNormKey)

            val scored = scoreCandidate(seed, candidate, userProfile)
            if (scored.score > 0) {
                scoredList.add(scored)
            }
        }

        // Sort descending by calculated score
        scoredList.sortByDescending { it.score }

        // Enforce artist diversity: maximum MAX_TRACKS_PER_ARTIST per artist
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<Song>()

        for (scored in scoredList) {
            val artistKey = scored.song.artist.trim().lowercase()
            val currentCount = artistCounts.getOrDefault(artistKey, 0)
            if (currentCount < MAX_TRACKS_PER_ARTIST) {
                result.add(scored.song)
                artistCounts[artistKey] = currentCount + 1
            }
            if (result.size >= limit) break
        }

        // If results are still below desired limit due to strict diversity, backfill remaining best scored
        if (result.size < limit) {
            for (scored in scoredList) {
                if (!result.contains(scored.song)) {
                    result.add(scored.song)
                }
                if (result.size >= limit) break
            }
        }

        return result
    }

    /**
     * Scores a single candidate song relative to the seed track and user profile.
     */
    fun scoreCandidate(
        seed: Song,
        candidate: Song,
        profile: UserPreferenceProfile
    ): ScoredCandidate {
        var score = 10.0 // Base baseline score for eligible tracks
        val reasons = mutableListOf<String>()

        val seedArtistNorm = seed.artist.trim().lowercase()
        val candidateArtistNorm = candidate.artist.trim().lowercase()

        // 1. Artist Affinity
        if (seedArtistNorm == candidateArtistNorm) {
            score += SCORE_SAME_ARTIST
            reasons.add("Same Artist")
        } else if (profile.favoriteArtists.contains(candidateArtistNorm)) {
            score += SCORE_FAVORITE_ARTIST
            reasons.add("Favorite Artist")
        }

        // 2. Genre / Style Compatibility
        val seedGenre = extractGenre(seed)
        val candidateGenre = extractGenre(candidate)
        if (seedGenre.isNotBlank() && candidateGenre.isNotBlank()) {
            if (seedGenre.equals(candidateGenre, ignoreCase = true)) {
                score += SCORE_EXACT_GENRE
                reasons.add("Exact Genre: $seedGenre")
            } else if (areGenresCompatible(seedGenre, candidateGenre)) {
                score += SCORE_COMPATIBLE_GENRE
                reasons.add("Compatible Genre ($seedGenre ~ $candidateGenre)")
            }
        }

        // 3. Cultural / Language Affinity (Tamil / Bollywood / Western)
        val isSeedTamil = hasTamilAffinity(seed)
        val isCandidateTamil = hasTamilAffinity(candidate)
        if (isSeedTamil && isCandidateTamil) {
            score += SCORE_LANGUAGE_AFFINITY
            reasons.add("Tamil Music Affinity")
        }
        if (profile.preferredLanguage.equals("Tamil", ignoreCase = true) && isCandidateTamil) {
            score += 25.0
            reasons.add("Preferred Language Match (Tamil)")
        }

        val isSeedBollywood = hasBollywoodAffinity(seed)
        val isCandidateBollywood = hasBollywoodAffinity(candidate)
        if (isSeedBollywood && isCandidateBollywood) {
            score += SCORE_LANGUAGE_AFFINITY
            reasons.add("Bollywood Music Affinity")
        }

        // 4. Mood, Vibe & Acoustic Keywords in Title/Album
        val sharedTokens = extractMatchingTokens(seed, candidate)
        if (sharedTokens.isNotEmpty()) {
            score += (sharedTokens.size * SCORE_KEYWORD_MATCH).coerceAtMost(40.0)
            reasons.add("Shared Vibes: ${sharedTokens.joinToString()}")
        }

        // 5. User Behavioral Feedback
        if (profile.favoriteSongIds.contains(candidate.id)) {
            score += SCORE_FAVORITE_SONG
            reasons.add("User Favorite")
        }

        val replays = profile.playCounts.getOrDefault(candidate.id, 0)
        if (replays >= 2) {
            score += SCORE_REPLAY_BONUS
            reasons.add("Frequently Replayed ($replays plays)")
        }

        if (profile.skippedSongIds.contains(candidate.id)) {
            score += PENALTY_SKIPPED
            reasons.add("Previously Skipped")
        }

        val recentIndex = profile.recentSongIds.indexOf(candidate.id)
        if (recentIndex in 0..2) {
            score += PENALTY_RECENTLY_PLAYED
            reasons.add("Just Played Recently")
        }

        // 6. Provider Quality Signal (Reward authorized full-length recordings over 30s clips)
        if (!candidate.isPreview && candidate.provider == Song.PROVIDER_AUDIUS) {
            score += SCORE_FULL_TRACK_BONUS
            reasons.add("Authorized Full Track")
        }

        return ScoredCandidate(candidate, score.coerceAtLeast(0.0), reasons)
    }

    private fun extractGenre(song: Song): String {
        val text = "${song.album} ${song.title}".lowercase()
        for (g in listOf("tamil", "bollywood", "electronic", "dance", "house", "techno", "edm", "hip-hop", "rap", "rock", "alternative", "pop", "r&b", "soul", "indie", "classical")) {
            if (text.contains(g)) return g
        }
        return ""
    }

    private fun areGenresCompatible(genreA: String, genreB: String): Boolean {
        val a = genreA.lowercase()
        val b = genreB.lowercase()
        return COMPATIBLE_GENRES[a]?.contains(b) == true || COMPATIBLE_GENRES[b]?.contains(a) == true
    }

    private fun hasTamilAffinity(song: Song): Boolean {
        val combined = "${song.artist} ${song.title} ${song.album}".lowercase()
        return TAMIL_SIGNALS.any { combined.contains(it) }
    }

    private fun hasBollywoodAffinity(song: Song): Boolean {
        val combined = "${song.artist} ${song.title} ${song.album}".lowercase()
        return BOLLYWOOD_SIGNALS.any { combined.contains(it) }
    }

    private fun extractMatchingTokens(songA: Song, songB: Song): Set<String> {
        val tokensA = "${songA.title} ${songA.album}".lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it in MOOD_TOKENS }
            .toSet()

        val tokensB = "${songB.title} ${songB.album}".lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it in MOOD_TOKENS }
            .toSet()

        return tokensA.intersect(tokensB)
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanArtist = artist.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${cleanTitle}_$cleanArtist"
    }
}
