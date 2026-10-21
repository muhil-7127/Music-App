package com.muhil.music.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class MuhilDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_lyrics` (
                        `songId` INTEGER NOT NULL,
                        `lyricsText` TEXT NOT NULL,
                        `isTimestamped` INTEGER NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`songId`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
