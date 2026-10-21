package com.muhil.music.data.repository

import com.muhil.music.data.local.dao.*
import com.muhil.music.data.local.entity.*
import com.muhil.music.data.mediastore.MediaStoreDataSource
import com.muhil.music.data.remote.AudiusMusicDataSource
import com.muhil.music.data.remote.OnlineLyricsDataSource
import com.muhil.music.data.remote.OnlineMusicDataSource
import com.muhil.music.domain.model.*
import com.muhil.music.domain.recommendation.MusicRecommendationEngine
import com.muhil.music.domain.recommendation.UserPreferenceProfile
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
    private val audiusMusic: AudiusMusicDataSource,
    private val onlineMusic: OnlineMusicDataSource,
    private val onlineLyrics: OnlineLyricsDataSource,
    private val mediaStore: MediaStoreDataSource,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val playlistDao: PlaylistDao,
    private val lyricsDao: LyricsDao,
    private val recommendationEngine: MusicRecommendationEngine
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
    private val recommendationsCache = ConcurrentHashMap<Long, CacheEntry<List<Song>>>()

    private val ttlFeaturedMs = 10 * 60 * 1000L // 10 minutes
    private val ttlGenreMs = 10 * 60 * 1000L
    private val ttlMetaMs = 15 * 60 * 1000L
    private val ttlSearchMs = 5 * 60 * 1000L
    private val ttlRecommendationsMs = 10 * 60 * 1000L

    // ==========================================
    // Online Music (Primary Source: Audius Full Tracks + iTunes Fallback)
    // ==========================================

    suspend fun getFeaturedSongs(forceRefresh: Boolean = false): List<Song> = coroutineScope {
        if (!forceRefresh) {
            featuredCache?.let { if (it.isValid(ttlFeaturedMs)) return@coroutineScope it.data }
        }

        val audiusDeferred = async { audiusMusic.getTrendingTracks(limit = 25) }
        val itunesDeferred = async { onlineMusic.getFeaturedSongs() }

        val audiusTracks = try { audiusDeferred.await() } catch (e: Exception) { emptyList() }
        val itunesTracks = try { itunesDeferred.await() } catch (e: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Song>()

        // Prioritize authorized full-length Audius tracks
        for (song in audiusTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }
        // Supplement with iTunes catalog tracks
        for (song in itunesTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }

        // Offline / Network Failure Fallback: use database favorites and recents to prevent blank screen
        if (combined.isEmpty()) {
            val recent = try { recentlyPlayedDao.getRecentlyPlayed().first().map { it.toSong() } } catch (e: Exception) { emptyList() }
            val favorites = try { favoriteDao.getAllFavorites().first().map { it.toSong() } } catch (e: Exception) { emptyList() }
            val fallback = (recent + favorites).distinctBy { normalizeKey(it.title, it.artist) }
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

        val audiusDeferred = async { audiusMusic.getTrendingTracksByGenre(genre, limit = 25) }
        val itunesDeferred = async { onlineMusic.getGenreSongs(genre) }

        val audiusTracks = try { audiusDeferred.await() } catch (e: Exception) { emptyList() }
        val itunesTracks = try { itunesDeferred.await() } catch (e: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Song>()

        for (song in audiusTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }
        for (song in itunesTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }

        if (combined.isNotEmpty()) {
            genreCache[cacheKey] = CacheEntry(combined)
        }
        combined
    }

    suspend fun searchOnlineSongs(query: String): List<Song> = coroutineScope {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return@coroutineScope emptyList()

        searchSongsCache[cleanQuery]?.let { if (it.isValid(ttlSearchMs)) return@coroutineScope it.data }

        val audiusDeferred = async { audiusMusic.searchSongs(query, limit = 30) }
        val itunesDeferred = async { onlineMusic.searchSongs(query, limit = 30) }

        val audiusTracks = try { audiusDeferred.await() } catch (e: Exception) { emptyList() }
        val itunesTracks = try { itunesDeferred.await() } catch (e: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Song>()

        for (song in audiusTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }
        for (song in itunesTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }

        if (combined.isNotEmpty()) {
            searchSongsCache[cleanQuery] = CacheEntry(combined)
        }
        combined
    }

    suspend fun searchOnlineArtists(query: String): List<Artist> = coroutineScope {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return@coroutineScope emptyList()

        searchArtistsCache[cleanQuery]?.let { if (it.isValid(ttlSearchMs)) return@coroutineScope it.data }

        val audiusDeferred = async { audiusMusic.searchArtists(query, limit = 15) }
        val itunesDeferred = async { onlineMusic.searchArtists(query, limit = 15) }

        val audiusArtists = try { audiusDeferred.await() } catch (e: Exception) { emptyList() }
        val itunesArtists = try { itunesDeferred.await() } catch (e: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Artist>()

        for (artist in audiusArtists) {
            val key = artist.name.trim().lowercase()
            if (seen.add(key)) combined.add(artist)
        }
        for (artist in itunesArtists) {
            val key = artist.name.trim().lowercase()
            if (seen.add(key)) combined.add(artist)
        }

        if (combined.isNotEmpty()) {
            searchArtistsCache[cleanQuery] = CacheEntry(combined)
        }
        combined
    }

    suspend fun searchOnlineAlbums(query: String): List<Album> = coroutineScope {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return@coroutineScope emptyList()

        searchAlbumsCache[cleanQuery]?.let { if (it.isValid(ttlSearchMs)) return@coroutineScope it.data }

        val audiusDeferred = async { audiusMusic.searchPlaylists(query, limit = 15) }
        val itunesDeferred = async { onlineMusic.searchAlbums(query, limit = 15) }

        val audiusPlaylists = try { audiusDeferred.await() } catch (e: Exception) { emptyList() }
        val itunesAlbums = try { itunesDeferred.await() } catch (e: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Album>()

        for (album in audiusPlaylists) {
            val key = album.title.trim().lowercase()
            if (seen.add(key)) combined.add(album)
        }
        for (album in itunesAlbums) {
            val key = album.title.trim().lowercase()
            if (seen.add(key)) combined.add(album)
        }

        if (combined.isNotEmpty()) {
            searchAlbumsCache[cleanQuery] = CacheEntry(combined)
        }
        combined
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
        val itunesAlbums = try { onlineMusic.getTrendingAlbums() } catch (e: Exception) { emptyList() }
        if (itunesAlbums.isNotEmpty()) {
            albumsCache = CacheEntry(itunesAlbums)
        }
        itunesAlbums
    }

    /**
     * Generates intelligent, multi-signal similar song recommendations based on a seed song.
     */
    suspend fun getRecommendations(seed: Song, limit: Int = 15): List<Song> = coroutineScope {
        recommendationsCache[seed.id]?.let {
            if (it.isValid(ttlRecommendationsMs)) return@coroutineScope it.data
        }

        // 1. Gather candidate pool across providers concurrently
        val artistQuery = seed.artist.trim()
        val genre = if (seed.album.isNotBlank()) seed.album else "Pop"

        val artistAudiusDef = async { audiusMusic.searchSongs(artistQuery, limit = 15) }
        val artistItunesDef = async { onlineMusic.searchSongs(artistQuery, limit = 15) }
        val genreAudiusDef = async { audiusMusic.getTrendingTracksByGenre(genre, limit = 20) }
        val genreItunesDef = async { onlineMusic.getGenreSongs(genre, limit = 20) }
        val localSongsDef = async { try { mediaStore.getAllSongs() } catch (e: Exception) { emptyList() } }

        val rawCandidates = mutableListOf<Song>()
        rawCandidates.addAll(try { artistAudiusDef.await() } catch (e: Exception) { emptyList() })
        rawCandidates.addAll(try { artistItunesDef.await() } catch (e: Exception) { emptyList() })
        rawCandidates.addAll(try { genreAudiusDef.await() } catch (e: Exception) { emptyList() })
        rawCandidates.addAll(try { genreItunesDef.await() } catch (e: Exception) { emptyList() })
        rawCandidates.addAll(try { localSongsDef.await() } catch (e: Exception) { emptyList() })

        // Supplement with cached featured songs if candidate pool is small
        featuredCache?.data?.let { rawCandidates.addAll(it) }

        // 2. Fetch User Preference Profile
        val favSongs = try { favoriteDao.getAllFavorites().first() } catch (e: Exception) { emptyList() }
        val recentSongs = try { recentlyPlayedDao.getRecentlyPlayed().first() } catch (e: Exception) { emptyList() }

        val userProfile = UserPreferenceProfile(
            favoriteSongIds = favSongs.map { it.songId }.toSet(),
            favoriteArtists = favSongs.map { it.artist.trim().lowercase() }.toSet(),
            recentSongIds   = recentSongs.map { it.songId },
            playCounts      = recentSongs.groupingBy { it.songId }.eachCount()
        )

        // 3. Delegate to Recommendation Engine for scoring, diversity capping, and deduplication
        val ranked = recommendationEngine.rankRecommendations(seed, rawCandidates, userProfile, limit)

        if (ranked.isNotEmpty()) {
            recommendationsCache[seed.id] = CacheEntry(ranked)
        }
        ranked
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
