package com.muhil.music.data.repository

import com.muhil.music.data.local.dao.*
import com.muhil.music.data.local.entity.*
import com.muhil.music.data.local.preferences.UserPreferencesManager
import com.muhil.music.data.mediastore.MediaStoreDataSource
import com.muhil.music.data.remote.AudiusMusicDataSource
import com.muhil.music.data.remote.JioSaavnMusicDataSource
import com.muhil.music.data.remote.OnlineLyricsDataSource
import com.muhil.music.data.remote.OnlineMusicDataSource
import com.muhil.music.domain.model.*
import com.muhil.music.domain.recommendation.Mood
import com.muhil.music.domain.recommendation.MusicRecommendationEngine
import com.muhil.music.domain.recommendation.UserPreferenceProfile
import com.muhil.music.domain.search.MovieSoundtrack
import com.muhil.music.domain.search.MusicSearchRanker
import com.muhil.music.domain.search.QueryIntent
import com.muhil.music.domain.search.RankedSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val jioSaavnMusic: JioSaavnMusicDataSource,
    private val audiusMusic: AudiusMusicDataSource,
    private val onlineMusic: OnlineMusicDataSource,
    private val onlineLyrics: OnlineLyricsDataSource,
    private val mediaStore: MediaStoreDataSource,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val playlistDao: PlaylistDao,
    private val lyricsDao: LyricsDao,
    private val recommendationEngine: MusicRecommendationEngine,
    private val searchRanker: MusicSearchRanker,
    val userPreferencesManager: UserPreferencesManager
) {

    // ==========================================
    // In-Memory Caching with TTL for Instant Loading
    // ==========================================

    private class CacheEntry<T>(val data: T, val timestamp: Long = System.currentTimeMillis()) {
        fun isValid(ttlMs: Long): Boolean = (System.currentTimeMillis() - timestamp) < ttlMs
    }

    private var featuredCache: CacheEntry<List<Song>>? = null
    private val genreCache = ConcurrentHashMap<String, CacheEntry<List<Song>>>()
    private var artistsCache: CacheEntry<List<Artist>>? = null
    private var albumsCache: CacheEntry<List<Album>>? = null
    private val searchSongsCache = ConcurrentHashMap<String, CacheEntry<List<Song>>>()
    private val searchArtistsCache = ConcurrentHashMap<String, CacheEntry<List<Artist>>>()
    private val searchAlbumsCache = ConcurrentHashMap<String, CacheEntry<List<Album>>>()
    private val searchRankedCache = ConcurrentHashMap<String, CacheEntry<RankedSearchResult>>()
    private val recommendationsCache = ConcurrentHashMap<Long, CacheEntry<List<Song>>>()

    private var tamilHitsCache: CacheEntry<List<Song>>? = null
    private var tamilSoundtracksCache: CacheEntry<List<MovieSoundtrack>>? = null
    private var tamilClassicsCache: CacheEntry<List<Song>>? = null
    private var tamilMelodiesCache: CacheEntry<List<Song>>? = null

    private val ttlFeaturedMs = 10 * 60 * 1000L // 10 minutes
    private val ttlGenreMs = 10 * 60 * 1000L
    private val ttlMetaMs = 15 * 60 * 1000L
    private val ttlSearchMs = 5 * 60 * 1000L
    private val ttlRecommendationsMs = 10 * 60 * 1000L

    // ==========================================
    // Online Music (100% Authorized Full Tracks: JioSaavn + Audius)
    // ==========================================

    suspend fun getFeaturedSongs(forceRefresh: Boolean = false): List<Song> = coroutineScope {
        if (!forceRefresh) {
            featuredCache?.let { if (it.isValid(ttlFeaturedMs)) return@coroutineScope it.data }
        }

        val saavnDeferred = async { jioSaavnMusic.searchSongs("Tamil Top Hits 2024", limit = 25) }
        val audiusDeferred = async { audiusMusic.getTrendingTracks(limit = 25) }

        val saavnTracks = try { saavnDeferred.await() } catch (_: Exception) { emptyList() }
        val audiusTracks = try { audiusDeferred.await() } catch (_: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Song>()

        // 100% Full-length songs only: filter out any preview or empty URI
        for (song in (saavnTracks + audiusTracks)) {
            if (!song.isPreview && song.uri.isNotBlank()) {
                val key = normalizeKey(song.title, song.artist)
                if (seen.add(key)) combined.add(song)
            }
        }

        // Offline / Network Failure Fallback: use database favorites and recents
        if (combined.isEmpty()) {
            val recent = try { recentlyPlayedDao.getRecentlyPlayed().first().map { it.toSong() } } catch (_: Exception) { emptyList() }
            val favorites = try { favoriteDao.getAllFavorites().first().map { it.toSong() } } catch (_: Exception) { emptyList() }
            val fallback = (recent + favorites).filter { !it.isPreview && it.uri.isNotBlank() }.distinctBy { normalizeKey(it.title, it.artist) }
            if (fallback.isNotEmpty()) {
                return@coroutineScope fallback
            }
        }

        if (combined.isNotEmpty()) {
            featuredCache = CacheEntry(combined)
        }
        combined
    }

    suspend fun getGenreSongs(genre: String, forceRefresh: Boolean = false): List<Song> = coroutineScope {
        val cacheKey = genre.lowercase().trim()
        if (!forceRefresh) {
            genreCache[cacheKey]?.let { if (it.isValid(ttlGenreMs)) return@coroutineScope it.data }
        }

        val saavnDeferred = async { jioSaavnMusic.searchSongs(genre, limit = 25) }
        val audiusDeferred = async { audiusMusic.getTrendingTracksByGenre(genre, limit = 25) }

        val saavnTracks = try { saavnDeferred.await() } catch (_: Exception) { emptyList() }
        val audiusTracks = try { audiusDeferred.await() } catch (_: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Song>()

        for (song in (saavnTracks + audiusTracks)) {
            if (!song.isPreview && song.uri.isNotBlank()) {
                val key = normalizeKey(song.title, song.artist)
                if (seen.add(key)) combined.add(song)
            }
        }

        if (combined.isNotEmpty()) {
            genreCache[cacheKey] = CacheEntry(combined)
        }
        combined
    }

    /**
     * Precision search ranking: fetches candidate pool across JioSaavn (full tracks), Audius (full tracks),
     * and local music, then ranks using MusicSearchRanker.
     * Guarantees 100% full song playback with zero 30s previews.
     */
    suspend fun searchOnlineRanked(
        query: String,
        preferredLanguage: String = "Tamil"
    ): RankedSearchResult = coroutineScope {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@coroutineScope RankedSearchResult(
                topMatch = null,
                movieSoundtrack = null,
                rankedSongs = emptyList(),
                allSoundtracks = emptyList(),
                artists = emptyList(),
                albums = emptyList(),
                intent = QueryIntent.GENERAL
            )
        }

        val cacheKey = "${cleanQuery.lowercase()}_$preferredLanguage"
        searchRankedCache[cacheKey]?.let {
            if (it.isValid(ttlSearchMs)) return@coroutineScope it.data
        }

        val transliterated = searchRanker.transliterateTamil(cleanQuery)
        val hasTransliteration = transliterated != cleanQuery

        // Parallel queries to JioSaavn and Audius for full-length streams
        val saavnSongsDef = async { jioSaavnMusic.searchSongs(cleanQuery, limit = 35) }
        val transliteratedSaavnDef = if (hasTransliteration) {
            async { jioSaavnMusic.searchSongs(transliterated, limit = 25) }
        } else null
        val audiusSongsDef = async { audiusMusic.searchSongs(cleanQuery, limit = 25) }
        val localSongsDef = async { try { mediaStore.searchSongs(cleanQuery) } catch (_: Exception) { emptyList() } }

        val saavnAlbumsDef = async { jioSaavnMusic.searchAlbums(cleanQuery, limit = 15) }
        val audiusAlbumsDef = async { audiusMusic.searchPlaylists(cleanQuery, limit = 15) }

        val audiusArtistsDef = async { audiusMusic.searchArtists(cleanQuery, limit = 15) }
        val itunesArtistsDef = async { onlineMusic.searchArtists(cleanQuery, limit = 15, country = "IN") }

        val rawSongs = mutableListOf<Song>()
        rawSongs.addAll(try { saavnSongsDef.await() } catch (_: Exception) { emptyList() })
        if (transliteratedSaavnDef != null) {
            rawSongs.addAll(try { transliteratedSaavnDef.await() } catch (_: Exception) { emptyList() })
        }
        rawSongs.addAll(try { audiusSongsDef.await() } catch (_: Exception) { emptyList() })
        rawSongs.addAll(try { localSongsDef.await() } catch (_: Exception) { emptyList() })

        // STRICT FILTER: Zero Previews! Only full-length playable tracks!
        val fullSongs = rawSongs.filter { !it.isPreview && it.uri.isNotBlank() }

        val rawArtists = mutableListOf<Artist>()
        rawArtists.addAll(try { audiusArtistsDef.await() } catch (_: Exception) { emptyList() })
        rawArtists.addAll(try { itunesArtistsDef.await() } catch (_: Exception) { emptyList() })

        val rawAlbums = mutableListOf<Album>()
        rawAlbums.addAll(try { saavnAlbumsDef.await() } catch (_: Exception) { emptyList() })
        rawAlbums.addAll(try { audiusAlbumsDef.await() } catch (_: Exception) { emptyList() })

        val favIds = try { favoriteDao.getAllFavorites().first().map { it.songId }.toSet() } catch (_: Exception) { emptySet() }
        val songsWithFav = fullSongs.map { if (it.id in favIds) it.copy(isFavorite = true) else it }

        val ranked = searchRanker.rankSearchResults(
            query = cleanQuery,
            candidates = songsWithFav,
            artists = rawArtists,
            albums = rawAlbums,
            preferredLanguage = preferredLanguage
        )

        searchRankedCache[cacheKey] = CacheEntry(ranked)
        ranked
    }

    suspend fun searchOnlineSongs(query: String): List<Song> {
        return searchOnlineRanked(query).songs
    }

    suspend fun searchOnlineArtists(query: String): List<Artist> {
        return searchOnlineRanked(query).artists
    }

    suspend fun searchOnlineAlbums(query: String): List<Album> {
        return searchOnlineRanked(query).albums
    }

    // ==========================================
    // Tamil-First Music Discovery
    // ==========================================

    suspend fun getTamilHits(forceRefresh: Boolean = false): List<Song> = coroutineScope {
        if (!forceRefresh) {
            tamilHitsCache?.let { if (it.isValid(ttlFeaturedMs)) return@coroutineScope it.data }
        }
        val saavnDef = async { jioSaavnMusic.getTamilHits(limit = 35) }
        val audiusDef = async { audiusMusic.searchSongs("Tamil Hits", limit = 15) }

        val saavnList = try { saavnDef.await() } catch (_: Exception) { emptyList() }
        val audiusList = try { audiusDef.await() } catch (_: Exception) { emptyList() }

        val all = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        for (s in (saavnList + audiusList)) {
            if (!s.isPreview && s.uri.isNotBlank()) {
                val key = normalizeKey(s.title, s.artist)
                if (seen.add(key)) all.add(s)
            }
        }

        if (all.isNotEmpty()) {
            tamilHitsCache = CacheEntry(all)
        }
        all
    }

    suspend fun getTamilSoundtracks(forceRefresh: Boolean = false): List<MovieSoundtrack> = coroutineScope {
        if (!forceRefresh) {
            tamilSoundtracksCache?.let { if (it.isValid(ttlMetaMs)) return@coroutineScope it.data }
        }
        val soundtracks = try {
            jioSaavnMusic.getTamilSoundtracks(limit = 8)
        } catch (_: Exception) {
            emptyList()
        }

        if (soundtracks.isNotEmpty()) {
            tamilSoundtracksCache = CacheEntry(soundtracks)
        }
        soundtracks
    }

    suspend fun getTamilClassics(forceRefresh: Boolean = false): List<Song> = coroutineScope {
        if (!forceRefresh) {
            tamilClassicsCache?.let { if (it.isValid(ttlFeaturedMs)) return@coroutineScope it.data }
        }
        val classics = try {
            jioSaavnMusic.getTamilClassics(limit = 30)
                .filter { !it.isPreview && it.uri.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }

        val seen = mutableSetOf<String>()
        val all = mutableListOf<Song>()
        for (s in classics) {
            val key = normalizeKey(s.title, s.artist)
            if (seen.add(key)) all.add(s)
        }

        if (all.isNotEmpty()) {
            tamilClassicsCache = CacheEntry(all)
        }
        all
    }

    suspend fun getTamilMelodies(forceRefresh: Boolean = false): List<Song> = coroutineScope {
        if (!forceRefresh) {
            tamilMelodiesCache?.let { if (it.isValid(ttlFeaturedMs)) return@coroutineScope it.data }
        }
        val melodies = try {
            jioSaavnMusic.getTamilMelodies(limit = 30)
                .filter { !it.isPreview && it.uri.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }

        val seen = mutableSetOf<String>()
        val all = mutableListOf<Song>()
        for (s in melodies) {
            val key = normalizeKey(s.title, s.artist)
            if (seen.add(key)) all.add(s)
        }

        if (all.isNotEmpty()) {
            tamilMelodiesCache = CacheEntry(all)
        }
        all
    }

    fun getTamilArtists(forceRefresh: Boolean = false): List<Artist> {
        return listOf(
            Artist(id = 543888365L, name = "Anirudh Ravichander", songCount = 85, albumCount = 20, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d1/aa/22/d1aa223d-249d-6490-25fc-8ea736dd6313/190296767357.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 49669528L, name = "A.R. Rahman", songCount = 120, albumCount = 45, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/c9/a1/38/c9a13809-53e7-2b36-efd3-90d56569165b/886443685608.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 265693527L, name = "Yuvan Shankar Raja", songCount = 95, albumCount = 30, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/aa/62/1f/aa621fa7-69b7-b08e-8971-419b16c529cb/8903431614749_cover.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 49669542L, name = "Harris Jayaraj", songCount = 70, albumCount = 22, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/36/53/7c/36537c35-26a9-8356-9a57-1d37a54a7c06/8902894356070_cover.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 5271810L, name = "Ilaiyaraaja", songCount = 350, albumCount = 90, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/d2/e6/bd/d2e6bd76-a0e2-658f-2877-3e1eeef94759/8902894359415_cover.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 907085775L, name = "Sid Sriram", songCount = 60, albumCount = 15, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/64/97/8f/64978ffc-1845-eec8-5be4-f5dc81df3e73/8902894359040_cover.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 49669530L, name = "Santhosh Narayanan", songCount = 55, albumCount = 18, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/7e/68/78/7e6878b2-540d-2b7e-9730-109ce4a34b3e/8903431623888_cover.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 5262706L, name = "S.P. Balasubrahmanyam", songCount = 500, albumCount = 120, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/55/aa/76/55aa7632-4752-ce2a-e6ae-f14d9b23adcb/8902894358890_cover.jpg/600x600bb.jpg", isOnline = true)
        )
    }

    suspend fun getTopArtists(forceRefresh: Boolean = false): List<Artist> = coroutineScope {
        if (!forceRefresh) {
            artistsCache?.let { if (it.isValid(ttlMetaMs)) return@coroutineScope it.data }
        }
        val itunesArtists = try { onlineMusic.getTopArtists() } catch (e: Exception) { emptyList() }
        if (itunesArtists.isNotEmpty()) {
            artistsCache = CacheEntry(itunesArtists)
        }
        itunesArtists
    }

    suspend fun getTrendingAlbums(forceRefresh: Boolean = false): List<Album> = coroutineScope {
        if (!forceRefresh) {
            albumsCache?.let { if (it.isValid(ttlMetaMs)) return@coroutineScope it.data }
        }
        val saavnAlbums = try { jioSaavnMusic.searchAlbums("Tamil Top Soundtrack", limit = 15) } catch (_: Exception) { emptyList() }
        if (saavnAlbums.isNotEmpty()) {
            albumsCache = CacheEntry(saavnAlbums)
            return@coroutineScope saavnAlbums
        }
        val itunesAlbums = try { onlineMusic.getTrendingAlbums() } catch (e: Exception) { emptyList() }
        if (itunesAlbums.isNotEmpty()) {
            albumsCache = CacheEntry(itunesAlbums)
        }
        itunesAlbums
    }

    /**
     * Generates intelligent, multi-signal similar song recommendations based on a seed song.
     * Guarantees 100% full song playback with zero previews.
     */
    suspend fun getRecommendations(
        seed: Song,
        currentMood: Mood? = null,
        limit: Int = 15
    ): List<Song> = coroutineScope {
        if (currentMood == null) {
            recommendationsCache[seed.id]?.let {
                if (it.isValid(ttlRecommendationsMs)) return@coroutineScope it.data
            }
        }

        // 1. Gather candidate pool across providers concurrently
        val artistQuery = seed.artist.trim()
        val genre = if (seed.album.isNotBlank()) seed.album else "Pop"

        val artistSaavnDef = async { jioSaavnMusic.searchSongs(artistQuery, limit = 15) }
        val artistAudiusDef = async { audiusMusic.searchSongs(artistQuery, limit = 15) }
        val genreSaavnDef = async { jioSaavnMusic.searchSongs(genre, limit = 20) }
        val genreAudiusDef = async { audiusMusic.getTrendingTracksByGenre(genre, limit = 20) }
        val localSongsDef = async { try { mediaStore.getAllSongs() } catch (e: Exception) { emptyList() } }

        val rawCandidates = mutableListOf<Song>()
        rawCandidates.addAll(try { artistSaavnDef.await() } catch (_: Exception) { emptyList() })
        rawCandidates.addAll(try { artistAudiusDef.await() } catch (_: Exception) { emptyList() })
        rawCandidates.addAll(try { genreSaavnDef.await() } catch (_: Exception) { emptyList() })
        rawCandidates.addAll(try { genreAudiusDef.await() } catch (_: Exception) { emptyList() })
        rawCandidates.addAll(try { localSongsDef.await() } catch (_: Exception) { emptyList() })

        // Supplement with cached featured songs if candidate pool is small
        featuredCache?.data?.let { rawCandidates.addAll(it) }

        // STRICT FILTER: ZERO PREVIEWS! Only genuine full tracks!
        val fullCandidates = rawCandidates.filter { !it.isPreview && it.uri.isNotBlank() }

        // 2. Fetch User Preference Profile
        val favSongs = try { favoriteDao.getAllFavorites().first() } catch (e: Exception) { emptyList() }
        val recentSongs = try { recentlyPlayedDao.getRecentlyPlayed().first() } catch (e: Exception) { emptyList() }
        val skippedSongs = try { userPreferencesManager.skippedSongIds.first() } catch (_: Exception) { emptySet() }
        val customPlayCounts = try { userPreferencesManager.playCounts.first() } catch (_: Exception) { emptyMap() }
        val prefLang = try { userPreferencesManager.preferredLanguage.first() } catch (_: Exception) { "Tamil" }
        val dislikedSongIds = try { userPreferencesManager.dislikedSongIds.first() } catch (_: Exception) { emptySet() }
        val dislikedArtists = try { userPreferencesManager.dislikedArtists.first() } catch (_: Exception) { emptySet() }

        val recentCounts = recentSongs.groupingBy { it.songId }.eachCount()
        val mergedPlayCounts = (recentCounts.keys + customPlayCounts.keys).associateWith {
            (recentCounts[it] ?: 0) + (customPlayCounts[it] ?: 0)
        }

        val userProfile = UserPreferenceProfile(
            favoriteSongIds   = favSongs.map { it.songId }.toSet(),
            favoriteArtists   = favSongs.map { it.artist.trim().lowercase() }.toSet(),
            recentSongIds     = recentSongs.map { it.songId },
            skippedSongIds    = skippedSongs,
            playCounts        = mergedPlayCounts,
            preferredLanguage = prefLang,
            currentMood       = currentMood,
            dislikedSongIds   = dislikedSongIds,
            dislikedArtists   = dislikedArtists
        )

        // 3. Delegate to Recommendation Engine for scoring, diversity capping, and deduplication
        val ranked = recommendationEngine.rankRecommendations(seed, fullCandidates, userProfile, limit)

        if (currentMood == null && ranked.isNotEmpty()) {
            recommendationsCache[seed.id] = CacheEntry(ranked)
        }
        ranked
    }

    /**
     * Generates pure, full-track music recommendations tailored to an explicit mood.
     */
    suspend fun getMoodRecommendations(mood: Mood, limit: Int = 15): List<Song> = coroutineScope {
        val query1 = when (mood) {
            Mood.ROMANTIC -> "Tamil Melody"
            Mood.RELAXED -> "Tamil Chill"
            Mood.HAPPY -> "Tamil Dance"
            Mood.ENERGETIC -> "Tamil Kuthu"
            Mood.PARTY -> "Party Hits"
            Mood.FOCUS -> "Instrumental Melody"
            Mood.SLEEP -> "Lofi Melody"
            Mood.SAD -> "Tamil Sad Songs"
            Mood.MOTIVATIONAL -> "Tamil Motivation"
        }
        val query2 = mood.keywords.firstOrNull() ?: mood.displayName

        val saavn1Def = async { jioSaavnMusic.searchSongs(query1, limit = 20) }
        val saavn2Def = async { jioSaavnMusic.searchSongs(query2, limit = 20) }
        val audiusDef = async { audiusMusic.getTrendingTracksByGenre(mood.genres.firstOrNull() ?: "pop", limit = 15) }

        val rawCandidates = mutableListOf<Song>()
        rawCandidates.addAll(try { saavn1Def.await() } catch (_: Exception) { emptyList() })
        rawCandidates.addAll(try { saavn2Def.await() } catch (_: Exception) { emptyList() })
        rawCandidates.addAll(try { audiusDef.await() } catch (_: Exception) { emptyList() })
        featuredCache?.data?.let { rawCandidates.addAll(it) }

        val fullCandidates = rawCandidates.filter { !it.isPreview && it.uri.isNotBlank() }

        val favSongs = try { favoriteDao.getAllFavorites().first() } catch (e: Exception) { emptyList() }
        val recentSongs = try { recentlyPlayedDao.getRecentlyPlayed().first() } catch (e: Exception) { emptyList() }
        val skippedSongs = try { userPreferencesManager.skippedSongIds.first() } catch (_: Exception) { emptySet() }
        val customPlayCounts = try { userPreferencesManager.playCounts.first() } catch (_: Exception) { emptyMap() }
        val prefLang = try { userPreferencesManager.preferredLanguage.first() } catch (_: Exception) { "Tamil" }
        val dislikedSongIds = try { userPreferencesManager.dislikedSongIds.first() } catch (_: Exception) { emptySet() }
        val dislikedArtists = try { userPreferencesManager.dislikedArtists.first() } catch (_: Exception) { emptySet() }

        val recentCounts = recentSongs.groupingBy { it.songId }.eachCount()
        val mergedPlayCounts = (recentCounts.keys + customPlayCounts.keys).associateWith {
            (recentCounts[it] ?: 0) + (customPlayCounts[it] ?: 0)
        }

        val userProfile = UserPreferenceProfile(
            favoriteSongIds   = favSongs.map { it.songId }.toSet(),
            favoriteArtists   = favSongs.map { it.artist.trim().lowercase() }.toSet(),
            recentSongIds     = recentSongs.map { it.songId },
            skippedSongIds    = skippedSongs,
            playCounts        = mergedPlayCounts,
            preferredLanguage = prefLang,
            currentMood       = mood,
            dislikedSongIds   = dislikedSongIds,
            dislikedArtists   = dislikedArtists
        )

        val pseudoSeed = fullCandidates.firstOrNull() ?: Song(
            id = -999L, title = mood.displayName, artist = "Various Artists", album = mood.displayName,
            duration = 180000L, albumArtUri = null, uri = "dummy", isOnline = true, provider = Song.PROVIDER_SAAVN
        )

        recommendationEngine.rankRecommendations(pseudoSeed, fullCandidates, userProfile, limit)
    }

    suspend fun recordDislikedSong(songId: Long) {
        userPreferencesManager.addDislikedSong(songId)
    }

    suspend fun recordDislikedArtist(artist: String) {
        userPreferencesManager.addDislikedArtist(artist)
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanArtist = artist.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${cleanTitle}_$cleanArtist"
    }

    // ==========================================
    // Local Music (Secondary Device Source)
    // ==========================================

    suspend fun getLocalSongs(): List<Song> = mediaStore.getAllSongs()

    suspend fun searchLocalSongs(query: String): List<Song> = mediaStore.searchSongs(query)

    suspend fun getLocalAlbums(): List<Album> {
        val songs = getLocalSongs()
        return songs.groupBy { it.album }.map { (albumName, albumSongs) ->
            Album(
                id        = albumSongs.first().id,
                title     = albumName,
                artist    = albumSongs.first().artist,
                artUri    = albumSongs.first().albumArtUri,
                songCount = albumSongs.size,
                isOnline  = false,
                provider  = Song.PROVIDER_LOCAL
            )
        }.sortedBy { it.title }
    }

    suspend fun getLocalArtists(): List<Artist> {
        val songs = getLocalSongs()
        return songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            Artist(
                id         = artistSongs.first().id,
                name       = artistName,
                songCount  = artistSongs.size,
                albumCount = artistSongs.map { it.album }.distinct().size,
                artUri     = artistSongs.first().albumArtUri,
                isOnline   = false,
                provider   = Song.PROVIDER_LOCAL
            )
        }.sortedBy { it.name }
    }

    // ==========================================
    // Favorites
    // ==========================================

    fun getFavoriteSongIds(): Flow<List<Long>> =
        favoriteDao.getAllFavorites().map { list -> list.map { it.songId } }

    fun getFavoriteSongs(): Flow<List<Song>> =
        favoriteDao.getAllFavorites().map { list -> list.map { it.toSong() } }

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun addFavorite(song: Song) {
        favoriteDao.addFavorite(
            FavoriteEntity(
                songId          = song.id,
                title           = song.title,
                artist          = song.artist,
                album           = song.album,
                albumArtUri     = song.albumArtUri,
                uri             = song.uri,
                duration        = song.duration,
                isOnline        = song.isOnline,
                isPreview       = song.isPreview,
                provider        = song.provider,
                providerTrackId = song.providerTrackId
            )
        )
    }

    suspend fun removeFavorite(songId: Long) = favoriteDao.removeFavorite(songId)

    // ==========================================
    // User Preferences & Listening Signals
    // ==========================================

    val preferredLanguage: Flow<String> get() = userPreferencesManager.preferredLanguage
    val searchHistory: Flow<List<String>> get() = userPreferencesManager.searchHistory

    suspend fun setPreferredLanguage(lang: String) = userPreferencesManager.setPreferredLanguage(lang)
    suspend fun saveSearchQuery(query: String) = userPreferencesManager.addSearchQuery(query)
    suspend fun removeSearchQuery(query: String) = userPreferencesManager.removeSearchQuery(query)
    suspend fun clearSearchHistory() = userPreferencesManager.clearSearchHistory()
    suspend fun recordSongSkipped(songId: Long) = userPreferencesManager.recordSkip(songId)
    suspend fun recordSongPlayed(songId: Long) = userPreferencesManager.recordPlay(songId)

    // ==========================================
    // Recently Played
    // ==========================================

    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>> =
        recentlyPlayedDao.getRecentlyPlayed()

    suspend fun addToRecentlyPlayed(song: Song) {
        recentlyPlayedDao.insert(
            RecentlyPlayedEntity(
                songId          = song.id,
                title           = song.title,
                artist          = song.artist,
                album           = song.album,
                albumArtUri     = song.albumArtUri,
                uri             = song.uri,
                duration        = song.duration,
                isOnline        = song.isOnline,
                isPreview       = song.isPreview,
                provider        = song.provider,
                providerTrackId = song.providerTrackId
            )
        )
        recentlyPlayedDao.trimOld()
    }

    // ==========================================
    // Playlists
    // ==========================================

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String, userId: String? = null): String {
        val id = UUID.randomUUID().toString()
        playlistDao.insertPlaylist(PlaylistEntity(id = id, name = name, userId = userId))
        return id
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.clearPlaylist(id)
        playlistDao.deletePlaylist(id)
    }

    fun getPlaylistSongs(playlistId: String): Flow<List<PlaylistSongEntity>> =
        playlistDao.getPlaylistSongs(playlistId)

    suspend fun addSongToPlaylist(playlistId: String, song: Song, position: Int = 0) {
        playlistDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId      = playlistId,
                songId          = song.id,
                position        = position,
                title           = song.title,
                artist          = song.artist,
                album           = song.album,
                uri             = song.uri,
                albumArtUri     = song.albumArtUri,
                duration        = song.duration,
                isOnline        = song.isOnline,
                isPreview       = song.isPreview,
                provider        = song.provider,
                providerTrackId = song.providerTrackId
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)

    // ==========================================
    // Lyrics (LRCLIB + Local Cache)
    // ==========================================

    suspend fun getLyrics(song: Song): LyricsState {
        // 1. Check local cache first
        val cached = lyricsDao.getLyrics(song.id)
        if (cached != null) {
            return if (cached.isTimestamped) {
                val lines = onlineLyrics.parseLrc(cached.lyricsText)
                if (lines.isNotEmpty()) LyricsState.Synced(lines) else LyricsState.Plain(cached.lyricsText)
            } else {
                LyricsState.Plain(cached.lyricsText)
            }
        }

        // 2. Fetch from online lyrics provider (LRCLIB) with accurate duration
        val onlineState = onlineLyrics.fetchLyrics(song.title, song.artist, song.duration)
        if (onlineState is LyricsState.Synced) {
            val raw = onlineState.lines.joinToString("\n") { "[${formatLrcTime(it.timestampMs)}]${it.text}" }
            lyricsDao.insertLyrics(CachedLyricsEntity(song.id, raw, isTimestamped = true))
            return onlineState
        } else if (onlineState is LyricsState.Plain) {
            lyricsDao.insertLyrics(CachedLyricsEntity(song.id, onlineState.text, isTimestamped = false))
            return onlineState
        }

        return LyricsState.Unavailable
    }

    private fun formatLrcTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val centi = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(min, sec, centi)
    }
}
