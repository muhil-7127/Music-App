package com.muhil.music.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,   // ms
    val uri: String,
    val albumArtUri: String? = null,
    val dateAdded: Long = 0L,
    val isFavorite: Boolean = false,
    val trackNumber: Int = 0,
    val size: Long = 0L,
    val isOnline: Boolean = false,
    val isPreview: Boolean = false,
    val genre: String? = null
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artUri: String? = null,
    val songCount: Int = 0,
    val year: Int = 0,
    val isOnline: Boolean = false
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val artUri: String? = null,
    val isOnline: Boolean = false
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isLocal: Boolean = true,
    val userId: String? = null
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

sealed class LyricsState {
    object Loading : LyricsState()
    object Unavailable : LyricsState()
    data class Plain(val text: String) : LyricsState()
    data class Synced(val lines: List<LyricLine>) : LyricsState()
    data class Error(val message: String) : LyricsState()
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

enum class RepeatMode { OFF, ALL, ONE }
