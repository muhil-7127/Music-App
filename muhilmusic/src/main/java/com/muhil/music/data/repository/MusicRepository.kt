package com.muhil.music.data.repository

import com.muhil.music.data.local.dao.*
import com.muhil.music.data.local.entity.*
import com.muhil.music.data.mediastore.MediaStoreDataSource
import com.muhil.music.data.remote.AudiusMusicDataSource
import com.muhil.music.data.remote.OnlineLyricsDataSource
import com.muhil.music.data.remote.OnlineMusicDataSource
import com.muhil.music.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
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
    private val lyricsDao: LyricsDao
) {

    // ==========================================
    // Online Music (Primary Source: Audius Full Tracks + iTunes Fallback)
    // ==========================================

    suspend fun getFeaturedSongs(): List<Song> = coroutineScope {
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
        combined
    }

    suspend fun getGenreSongs(genre: String): List<Song> = coroutineScope {
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
        combined
    }

    suspend fun searchOnlineSongs(query: String): List<Song> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
        val audiusDeferred = async { audiusMusic.searchSongs(query, limit = 30) }
        val itunesDeferred = async { onlineMusic.searchSongs(query, limit = 30) }

        val audiusTracks = try { audiusDeferred.await() } catch (e: Exception) { emptyList() }
        val itunesTracks = try { itunesDeferred.await() } catch (e: Exception) { emptyList() }

        val seen = mutableSetOf<String>()
        val combined = mutableListOf<Song>()

        // Full-length Audius tracks take priority
        for (song in audiusTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }
        // Clearly-labeled preview iTunes tracks for wider search catalog
        for (song in itunesTracks) {
            val key = normalizeKey(song.title, song.artist)
            if (seen.add(key)) combined.add(song)
        }
        combined
    }

    suspend fun searchOnlineArtists(query: String): List<Artist> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
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
        combined
    }

    suspend fun searchOnlineAlbums(query: String): List<Album> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
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
        combined
    }

    suspend fun getTopArtists(): List<Artist> = coroutineScope {
        val itunesArtists = try { onlineMusic.getTopArtists() } catch (e: Exception) { emptyList() }
        itunesArtists
    }

    suspend fun getTrendingAlbums(): List<Album> = coroutineScope {
        val itunesAlbums = try { onlineMusic.getTrendingAlbums() } catch (e: Exception) { emptyList() }
        itunesAlbums
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
