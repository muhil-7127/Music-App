package com.muhil.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val title: String = "",
    val artist: String = "",
    val albumArtUri: String? = null,
    val uri: String = ""
)

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
    val uri: String = "",
    val albumArtUri: String? = null,
    val duration: Long = 0L
)

@Entity(tableName = "cached_lyrics")
data class CachedLyricsEntity(
    @PrimaryKey val songId: Long,
    val lyricsText: String,
    val isTimestamped: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)
