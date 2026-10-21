package com.muhil.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.muhil.music.domain.model.Song

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtUri: String? = null,
    val uri: String = "",
    val duration: Long = 0L,
    val isOnline: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id          = songId,
        title       = title,
        artist      = artist,
        album       = album,
        albumArtUri = albumArtUri,
        uri         = uri,
        duration    = duration,
        isOnline    = isOnline,
        isPreview   = isOnline,
        isFavorite  = true
    )
}

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtUri: String? = null,
    val uri: String = "",
    val duration: Long = 0L,
    val isOnline: Boolean = false
) {
    fun toSong(): Song = Song(
        id          = songId,
        title       = title,
        artist      = artist,
        album       = album,
        albumArtUri = albumArtUri,
        uri         = uri,
        duration    = duration,
        isOnline    = isOnline,
        isPreview   = isOnline
    )
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String? = null
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val uri: String = "",
    val albumArtUri: String? = null,
    val duration: Long = 0L,
    val isOnline: Boolean = false
) {
    fun toSong(): Song = Song(
        id          = songId,
        title       = title,
        artist      = artist,
        album       = album,
        albumArtUri = albumArtUri,
        uri         = uri,
        duration    = duration,
        isOnline    = isOnline,
        isPreview   = isOnline
    )
}

@Entity(tableName = "cached_lyrics")
data class CachedLyricsEntity(
    @PrimaryKey val songId: Long,
    val lyricsText: String,
    val isTimestamped: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)
