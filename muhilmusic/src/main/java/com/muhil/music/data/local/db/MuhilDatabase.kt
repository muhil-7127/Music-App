package com.muhil.music.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muhil.music.data.local.dao.*
import com.muhil.music.data.local.entity.*

@Database(
    entities = [
        FavoriteEntity::class,
        RecentlyPlayedEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        CachedLyricsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MuhilDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao
}
