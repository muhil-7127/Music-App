package com.muhil.music.data.repository

import com.muhil.music.data.local.dao.*
import com.muhil.music.data.local.entity.*
import com.muhil.music.data.mediastore.MediaStoreDataSource
import com.muhil.music.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val playlistDao: PlaylistDao,
    private val lyricsDao: LyricsDao
) {
    // ---- MediaStore ----
    suspend fun getAllSongs(): List<Song> = mediaStore.getAllSongs()
    suspend fun searchSongs(query: String): List<Song> = mediaStore.searchSongs(query)

    suspend fun getAlbums(): List<Album> {
        val songs = getAllSongs()
        return songs.groupBy { it.album }.map { (albumName, albumSongs) ->
            Album(
                id        = albumSongs.first().id,
                title     = albumName,
                artist    = albumSongs.first().artist,
                artUri    = albumSongs.first().albumArtUri,
                songCount = albumSongs.size
            )
        }.sortedBy { it.title }
    }

    suspend fun getArtists(): List<Artist> {
        val songs = getAllSongs()
        return songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            Artist(
                id         = artistSongs.first().id,
                name       = artistName,
                songCount  = artistSongs.size,
                albumCount = artistSongs.map { it.album }.distinct().size,
                artUri     = artistSongs.first().albumArtUri
            )
        }.sortedBy { it.name }
    }

    suspend fun getAlbumSongs(albumTitle: String): List<Song> =
        getAllSongs().filter { it.album == albumTitle }.sortedBy { it.trackNumber }

    suspend fun getArtistSongs(artistName: String): List<Song> =
        getAllSongs().filter { it.artist == artistName }

    // ---- Favorites ----
    fun getFavoriteSongIds(): Flow<List<Long>> =
        favoriteDao.getAllFavorites().map { list -> list.map { it.songId } }

    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun addFavorite(songId: Long) = favoriteDao.addFavorite(FavoriteEntity(songId))
    suspend fun removeFavorite(songId: Long) = favoriteDao.removeFavorite(songId)

    // getFavoriteSongs is handled in ViewModel via Flow collection

    // ---- Recently Played ----
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>> =
        recentlyPlayedDao.getRecentlyPlayed()

    suspend fun addToRecentlyPlayed(song: Song) {
        recentlyPlayedDao.insert(
            RecentlyPlayedEntity(
                songId      = song.id,
                title       = song.title,
                artist      = song.artist,
                albumArtUri = song.albumArtUri,
                uri         = song.uri
            )
        )
        recentlyPlayedDao.trimOld()
    }

    // ---- Playlists ----
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
                playlistId  = playlistId,
                songId      = song.id,
                position    = position,
                title       = song.title,
                artist      = song.artist,
                uri         = song.uri,
                albumArtUri = song.albumArtUri,
                duration    = song.duration
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) =
        playlistDao.removeSongFromPlaylist(playlistId, songId)

    // ---- Lyrics ----
    suspend fun getCachedLyrics(songId: Long): CachedLyricsEntity? =
        lyricsDao.getLyrics(songId)

    suspend fun cacheLyrics(songId: Long, text: String, isTimestamped: Boolean) {
        lyricsDao.insertLyrics(CachedLyricsEntity(songId, text, isTimestamped))
    }
}
