package com.muhil.music.domain.search

import com.muhil.music.domain.model.Album
import com.muhil.music.domain.model.Artist
import com.muhil.music.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton

enum class QueryIntent {
    SONG,
    MOVIE,
    ARTIST,
    GENERAL
}

data class MovieSoundtrack(
    val movieName: String,
    val composer: String,
    val albumArtUri: String?,
    val tracks: List<Song>
) {
    val movieTitle: String get() = movieName
    val composerOrArtist: String get() = composer
}

data class RankedSearchResult(
    val topMatch: Song?,
    val movieSoundtrack: MovieSoundtrack?,
    val rankedSongs: List<Song>,
    val allSoundtracks: List<MovieSoundtrack> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val intent: QueryIntent = QueryIntent.GENERAL
) {
    val songs: List<Song> get() = rankedSongs
    val detectedIntent: QueryIntent get() = intent
}

data class ScoredSong(
    val song: Song,
    val score: Double,
    val matchTier: Int,
    val reason: String
)

/**
 * High-precision search ranking engine:
 * Classifies query intent, applies Unicode and transliteration normalizations,
 * computes multi-tier relevance scores, fuzzy matches spelling variations,
 * groups soundtrack releases, and prioritizes authentic exact results.
 */
@Singleton
class MusicSearchRanker @Inject constructor() {

    companion object {
        // Known Tamil and Indian movies for soundtrack query intent detection
        private val KNOWN_MOVIES = setOf(
            "leo", "vikram", "jailer", "master", "beast", "ponniyin selvan", "ps1", "ps2",
            "amaran", "vettaiyan", "goat", "the greatest of all time", "kanguva", "indian 2",
            "roja", "bombay", "alaipayuthey", "mankatha", "mersal", "bigil", "sarkar",
            "thuppakki", "kaththi", "theri", "viswasam", "petta", "kabali", "kaithi",
            "doctor", "don", "varisu", "thunivu", "love today", "dragon", "viduthalai",
            "raayan", "captain miller", "ayalaan", "vaathi", "karnan", "asuran", "vada chennai",
            "soorarai pottru", "jai bhim", "anbe sivam", "nayakan", "baasha", "padayappa",
            "sivaji", "enthiran", "2.0", "ghilli", "pokkiri", "vinnaithaandi varuvaayaa",
            "neethaane en vasantham", "vaaranam aayiram", "ayan", "ko", "chandramukhi",
            "anjaan", "masss", "biriyani", "aarambam", "billa", "billa 2", "aayirathil oruvan",
            "pudhupettai", "7aum arivu", "velaiilla pattadhari", "vip", "mari", "maari",
            "kodi", "vada chennai", "asuran", "karnan", "kantara", "kgf", "kgf 2", "rrr",
            "pushpa", "pushpa 2", "bahubali", "devara", "salaar", "kalki"
        )

        // Known prominent Tamil artists & composers
        private val KNOWN_ARTISTS = setOf(
            "anirudh", "anirudh ravichander", "a.r. rahman", "ar rahman", "rahman",
            "yuvan", "yuvan shankar raja", "harris jayaraj", "ilaiyaraaja", "ilayaraja",
            "santhosh narayanan", "sid sriram", "dhanush", "g.v. prakash", "gv prakash",
            "d. imman", "d imman", "vidyasagar", "deva", "hiphop tamizha", "sam cs",
            "s.p. balasubrahmanyam", "spb", "k.j. yesudas", "hariharan", "unni menon",
            "shreya ghoshal", "chinmayi", "jonita gandhi", "andrea jeremiah", "pradeep kumar",
            "anthony daasan", "sean roldan", "dhee", "shweta mohan", "sujatha", "chithra",
            "coldplay", "the weeknd", "taylor swift", "billie eilish", "ed sheeran", "dua lipa",
            "arijit singh", "pritam", "badshah", "neha kakkar"
        )

        // Tamil Unicode block range: \u0B80 - \u0BFF
        private val TAMIL_CHAR_REGEX = Regex("[\u0B80-\u0BFF]")

        // Tamil script to Latin transliteration mappings
        private val TAMIL_SCRIPT_MAP = mapOf(
            "விக்ரம்" to "vikram",
            "லியோ" to "leo",
            "ஜெயிலர்" to "jailer",
            "அனிருத்" to "anirudh",
            "ரஹ்மான்" to "rahman",
            "காதல்" to "kadhal",
            "மாஸ்டர்" to "master",
            "பீஸ்ட்" to "beast",
            "அமரன்" to "amaran",
            "வேட்டையன்" to "vettaiyan",
            "தளபதி" to "thalapathi",
            "வாத்தி" to "vaathi",
            "ஆரம்பம்" to "aarambam",
            "அரபிக் குத்து" to "arabic kuthu",
            "ஹலாமதி ஹபிபோ" to "halamithi habibo",
            "நா ரெடி" to "naa ready",
            "ஹுக்கும்" to "hukum",
            "காவாலா" to "kaavaalaa",
            "மெர்சல்" to "mersal",
            "பிகில்" to "bigil",
            "சர்கார்" to "sarkar",
            "துப்பாக்கி" to "thuppakki",
            "கத்தி" to "kaththi",
            "தெறி" to "theri",
            "ரோஜா" to "roja",
            "பம்பாய்" to "bombay",
            "அலைபாயுதே" to "alaipayuthey",
            "மின்சார கனவு" to "minsara kanavu",
            "படையப்பா" to "padayappa",
            "பாட்ஷா" to "baasha",
            "சிவாஜி" to "sivaji",
            "எந்திரன்" to "enthiran",
            "கைதி" to "kaithi",
            "பொன்னியின் செல்வன்" to "ponniyin selvan",
            "மங்காத்தா" to "mankatha",
            "கபாலி" to "kabali",
            "டாக்டர்" to "doctor"
        )

        // Canonical phonetic variations for transliterations
        private val PHONETIC_REPLACEMENTS = listOf(
            Regex("\\bkaadhal\\b|\\bkaathal\\b") to "kadhal",
            Regex("\\bporattam\\b|\\bporaattam\\b") to "porattam",
            Regex("\\baniruth\\b") to "anirudh",
            Regex("\\ba\\.r\\. rahman\\b|\\bar rahman\\b") to "ar rahman",
            Regex("\\bilayaraja\\b|\\billayaraja\\b") to "ilaiyaraaja",
            Regex("\\bharris jairaj\\b") to "harris jayaraj",
            Regex("\\bu1\\b") to "yuvan",
            Regex("\\bvathi\\b") to "vaathi",
            Regex("\\bvicram\\b") to "vikram",
            Regex("\\bhalmithi\\b") to "halamithi",
            Regex("\\bkoothu\\b") to "kuthu",
            Regex("\\bvizhiye\\b") to "vizhi",
            Regex("\\bkanne\\b") to "kanna"
        )
    }

    /**
     * Detects the probable search intent based on query analysis.
     */
    fun detectIntent(query: String): QueryIntent {
        val norm = normalize(query)
        val transliterated = transliterateTamil(norm)

        // Check Movie Intent
        if (KNOWN_MOVIES.contains(norm) || KNOWN_MOVIES.contains(transliterated)) {
            return QueryIntent.MOVIE
        }
        if (norm.contains("soundtrack") || norm.contains("ost") || norm.contains("movie") || norm.contains("film")) {
            return QueryIntent.MOVIE
        }

        // Check Artist Intent
        if (KNOWN_ARTISTS.contains(norm) || KNOWN_ARTISTS.contains(transliterated)) {
            return QueryIntent.ARTIST
        }

        // Check Song Intent: specific song keywords or quotes
        if (norm.startsWith("song ") || norm.endsWith(" song") || norm.contains("track") || query.startsWith("\"")) {
            return QueryIntent.SONG
        }

        return QueryIntent.GENERAL
    }

    /**
     * Primary entry point: Ranks all search results, isolates top match,
     * extracts movie soundtrack if applicable, deduplicates tracks, and sorts by relevance.
     */
    fun rankSearchResults(
        query: String,
        candidates: List<Song>,
        artists: List<Artist> = emptyList(),
        albums: List<Album> = emptyList(),
        preferredLanguage: String = "Tamil"
    ): RankedSearchResult {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return RankedSearchResult(
                topMatch = null,
                movieSoundtrack = null,
                rankedSongs = emptyList(),
                allSoundtracks = emptyList(),
                artists = emptyList(),
                albums = emptyList(),
                intent = QueryIntent.GENERAL
            )
        }

        val normQuery = normalize(trimmedQuery)
        val transliteratedQuery = transliterateTamil(normQuery)
        val canonQuery = canonicalize(transliteratedQuery)
        val intent = detectIntent(trimmedQuery)

        // 1. Deduplicate candidates across providers: keep the full-track or highest quality
        val deduplicatedCandidates = deduplicateCandidates(candidates)

        // 2. Score every candidate song
        val scoredSongs = deduplicatedCandidates.map { song ->
            scoreSong(song, normQuery, canonQuery, intent, preferredLanguage)
        }

        // 3. Sort descending by score
        val sortedScored = scoredSongs.sortedWith(
            compareByDescending<ScoredSong> { it.score }
                .thenByDescending { !it.song.isPreview } // Tie-break: full track first
                .thenByDescending { it.song.duration }
        )

        val rankedSongs = sortedScored.map { it.song }

        // 4. Identify Top Match (Hero Card)
        val highestScored = sortedScored.firstOrNull()
        val topMatch = if (highestScored != null && highestScored.score >= 650.0) {
            highestScored.song
        } else null

        // 5. Extract Movie Soundtrack View if applicable
        val movieSoundtrack = extractMovieSoundtrack(canonQuery, rankedSongs, intent)

        // 6. Rank artists and albums
        val sortedArtists = rankArtists(canonQuery, artists)
        val sortedAlbums = rankAlbums(canonQuery, albums)

        return RankedSearchResult(
            topMatch = topMatch,
            movieSoundtrack = movieSoundtrack,
            rankedSongs = rankedSongs,
            allSoundtracks = listOfNotNull(movieSoundtrack),
            artists = sortedArtists,
            albums = sortedAlbums,
            intent = intent
        )
    }

    /**
     * Overload for direct query and songs ranking used in tests and simple searches.
     */
    fun rankSearchResults(
        rawQuery: String,
        songs: List<Song>,
        preferredLanguage: String = "Tamil"
    ): RankedSearchResult = rankSearchResults(
        query = rawQuery,
        candidates = songs,
        artists = emptyList(),
        albums = emptyList(),
        preferredLanguage = preferredLanguage
    )

    /**
     * Computes multi-tier relevance score for a song against the user query.
     */
    fun scoreSong(
        song: Song,
        normQuery: String,
        canonQuery: String,
        intent: QueryIntent,
        preferredLanguage: String
    ): ScoredSong {
        val normTitle = normalize(song.title)
        val canonTitle = canonicalize(normTitle)

        val normArtist = normalize(song.artist)
        val canonArtist = canonicalize(normArtist)

        val normAlbum = normalize(song.album)
        val canonAlbum = canonicalize(normAlbum)

        var score = 0.0
        var tier = 7
        var reason = "Broader Result"

        // Tier 1: Exact Song Title Match (Score 1000.0+)
        if (canonTitle == canonQuery || normTitle == normQuery) {
            score = 1000.0
            tier = 1
            reason = "Exact Song Title Match"

            // Tier 2 Sub-bonus: Exact Title + Artist Match
            if (canonArtist.contains(canonQuery) || normQuery.contains(normArtist)) {
                score += 150.0
                reason = "Exact Song Title and Artist Match"
            }
        }
        // Tier 2: Movie / Soundtrack Match
        else if (intent == QueryIntent.MOVIE && (canonAlbum.contains(canonQuery) || normAlbum.contains(normQuery))) {
            score = 850.0
            tier = 2
            reason = "Movie Soundtrack Match"

            // Extra points if the album explicitly denotes motion picture soundtrack
            if (normAlbum.contains("motion picture") || normAlbum.contains("soundtrack") || normAlbum.contains("ost")) {
                score += 30.0
            }
        }
        // Tier 3: Very Close Title Match / Transliteration / Prefix Match
        else if (canonTitle.startsWith(canonQuery) || canonQuery.startsWith(canonTitle)) {
            score = 750.0
            tier = 3
            reason = "Close Title Prefix Match"
        }
        else {
            val fuzzySim = maxOf(
                fuzzySimilarity(canonQuery, canonTitle),
                fuzzySimilarity(normQuery, normTitle)
            )
            if (fuzzySim >= 0.82) {
                score = 650.0 + (fuzzySim * 60.0)
                tier = 3
                reason = "Fuzzy Title Match (${(fuzzySim * 100).toInt()}%)"
            }
            // Check if all query words exist in song title
            else if (containsAllTokens(canonTitle, canonQuery)) {
                score = 620.0
                tier = 3
                reason = "Title Contains All Query Words"
            }
            // Tier 4: Title Contains Query as substring
            else if (canonTitle.contains(canonQuery) || normTitle.contains(normQuery)) {
                score = 520.0
                tier = 4
                reason = "Title Contains Query"
            }
            // Tier 5: Artist Match
            else if (canonArtist == canonQuery || normArtist == normQuery) {
                score = 420.0
                tier = 5
                reason = "Exact Artist Match"
            } else if (canonArtist.contains(canonQuery) || normArtist.contains(normQuery)) {
                score = 360.0
                tier = 5
                reason = "Artist Contains Query"
            }
            // Tier 6: Album Match (non-movie query)
            else if (canonAlbum.contains(canonQuery) || normAlbum.contains(normQuery)) {
                score = 280.0
                tier = 6
                reason = "Album Match"
            }
            // Tier 7: Broader / Genre Match
            else {
                score = 100.0
                tier = 7
                reason = "Related Result"
            }
        }

        // Tie-breaker 1: Preferred Language Match (+25.0)
        if (preferredLanguage.equals("Tamil", ignoreCase = true) && hasTamilAffinity(song)) {
            score += 25.0
        }

        // Tie-breaker 2: Authorized Full Track Stream (+15.0)
        if (!song.isPreview) {
            score += 15.0
        }

        // Tie-breaker 3: Favorite Song (+10.0)
        if (song.isFavorite) {
            score += 10.0
        }

        // Penalty: Unwanted Karaoke / Ringtone / Cover / Tribute if user didn't ask for it (-180.0)
        val combinedText = "$normTitle $normArtist $normAlbum"
        val isKaraokeOrCover = listOf("karaoke", "ring tone", "ringtone", "tribute to", "originally performed by")
            .any { combinedText.contains(it) }

        if (isKaraokeOrCover && !normQuery.contains("karaoke") && !normQuery.contains("ringtone")) {
            score -= 180.0
            reason += " (Deprioritized Cover/Karaoke)"
        }

        return ScoredSong(song, score.coerceAtLeast(0.0), tier, reason)
    }

    /**
     * Extracts a grouped Movie Soundtrack when query or top results warrant it.
     */
    private fun extractMovieSoundtrack(
        canonQuery: String,
        rankedSongs: List<Song>,
        intent: QueryIntent
    ): MovieSoundtrack? {
        if (rankedSongs.isEmpty()) return null

        // Group tracks by album
        val albumGroups = rankedSongs.groupBy {
            normalize(it.album).replace("original motion picture soundtrack", "").trim()
        }

        // Look for the group matching query or with largest cluster
        val matchingGroup = albumGroups.entries.firstOrNull { (albumName, songs) ->
            (albumName.contains(canonQuery) || canonQuery.contains(albumName)) && songs.size >= 2
        } ?: if (intent == QueryIntent.MOVIE && albumGroups.isNotEmpty()) {
            albumGroups.entries.maxByOrNull { it.value.size }
        } else null

        return matchingGroup?.let { (albumName, tracks) ->
            if (tracks.size >= 2) {
                MovieSoundtrack(
                    movieName = tracks.first().album.replace(Regex("(?i)original motion picture soundtrack|soundtrack|ost"), "").trim().ifEmpty { tracks.first().album },
                    composer = tracks.first().artist,
                    albumArtUri = tracks.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri,
                    tracks = tracks
                )
            } else null
        }
    }

    /**
     * Deduplicates songs across providers (iTunes preview vs Audius full track).
     */
    fun deduplicateCandidates(candidates: List<Song>): List<Song> {
        val map = linkedMapOf<String, Song>()
        for (song in candidates) {
            val key = normalizeKey(song.title, song.artist)
            val existing = map[key]
            if (existing == null) {
                map[key] = song
            } else {
                // If existing is preview and new is full track, replace
                if (existing.isPreview && !song.isPreview) {
                    map[key] = song
                }
            }
        }
        return map.values.toList()
    }

    private fun rankArtists(canonQuery: String, artists: List<Artist>): List<Artist> {
        return artists.sortedWith(
            compareByDescending<Artist> {
                val norm = canonicalize(normalize(it.name))
                when {
                    norm == canonQuery -> 1000
                    norm.startsWith(canonQuery) -> 750
                    norm.contains(canonQuery) -> 500
                    else -> 100
                }
            }.thenByDescending { it.songCount }
        )
    }

    private fun rankAlbums(canonQuery: String, albums: List<Album>): List<Album> {
        return albums.sortedWith(
            compareByDescending<Album> {
                val norm = canonicalize(normalize(it.title))
                when {
                    norm == canonQuery -> 1000
                    norm.startsWith(canonQuery) -> 750
                    norm.contains(canonQuery) -> 500
                    else -> 100
                }
            }.thenByDescending { it.year }
        )
    }

    fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[,.()\\-_–—'\"/\\\\:;!&?\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun transliterateTamil(text: String): String {
        var result = text
        for ((tamil, english) in TAMIL_SCRIPT_MAP) {
            if (result.contains(tamil)) {
                result = result.replace(tamil, english)
            }
        }
        return result
    }

    fun canonicalize(text: String): String {
        var canon = text
        for ((regex, replacement) in PHONETIC_REPLACEMENTS) {
            canon = canon.replace(regex, replacement)
        }
        return canon
    }

    fun containsAllTokens(target: String, query: String): Boolean {
        val queryTokens = query.split(" ").filter { it.length >= 2 }
        if (queryTokens.isEmpty()) return false
        return queryTokens.all { target.contains(it) }
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun fuzzySimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        return 1.0 - (levenshteinDistance(s1, s2).toDouble() / maxLen)
    }

    private fun hasTamilAffinity(song: Song): Boolean {
        val text = "${song.artist} ${song.title} ${song.album}".lowercase()
        return KNOWN_ARTISTS.any { text.contains(it) } || KNOWN_MOVIES.any { text.contains(it) }
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanArtist = artist.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${cleanTitle}_$cleanArtist"
    }
}
