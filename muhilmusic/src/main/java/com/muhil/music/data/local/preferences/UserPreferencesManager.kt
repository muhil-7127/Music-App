package com.muhil.music.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muhil_music_preferences")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history_json")
        val KEY_SKIPPED_SONGS = stringSetPreferencesKey("skipped_song_ids")
        val KEY_PLAY_COUNTS = stringPreferencesKey("play_counts_json")
        val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val KEY_USER_NAME = stringPreferencesKey("user_display_name")
        val KEY_PROFILE_PICTURE_URI = stringPreferencesKey("profile_picture_uri")

        const val LANG_TAMIL = "Tamil"
        const val LANG_ENGLISH = "English"
        const val LANG_HINDI = "Hindi"
        const val LANG_ALL = "All"

        val SUPPORTED_LANGUAGES = listOf(LANG_TAMIL, LANG_ENGLISH, LANG_HINDI, LANG_ALL)
    }

    /**
     * Preferred music language (defaults to Tamil for Tamil-first experience).
     */
    val preferredLanguage: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferred language")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_PREFERRED_LANGUAGE] ?: LANG_TAMIL
        }

    suspend fun setPreferredLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PREFERRED_LANGUAGE] = language
        }
    }

    /**
     * Search history as an ordered list of recent queries (most recent first, max 15).
     */
    val searchHistory: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading search history")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonStr = preferences[KEY_SEARCH_HISTORY] ?: return@map emptyList()
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_SEARCH_HISTORY]
            val list = mutableListOf<String>()
            if (currentJson != null) {
                try {
                    val array = JSONArray(currentJson)
                    for (i in 0 until array.length()) {
                        list.add(array.getString(i))
                    }
                } catch (_: Exception) {}
            }
            // Remove if already exists, then add to front
            list.removeAll { it.equals(trimmed, ignoreCase = true) }
            list.add(0, trimmed)
            // Limit to 15 recent items
            val truncated = list.take(15)
            val newArray = JSONArray(truncated)
            preferences[KEY_SEARCH_HISTORY] = newArray.toString()
        }
    }

    suspend fun removeSearchQuery(query: String) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_SEARCH_HISTORY] ?: return@edit
            try {
                val array = JSONArray(currentJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.getString(i)
                    if (!item.equals(query.trim(), ignoreCase = true)) {
                        list.add(item)
                    }
                }
                preferences[KEY_SEARCH_HISTORY] = JSONArray(list).toString()
            } catch (_: Exception) {}
        }
    }

    suspend fun clearSearchHistory() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_SEARCH_HISTORY)
        }
    }

    /**
     * Set of song IDs explicitly skipped by the user.
     */
    val skippedSongIds: Flow<Set<Long>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_SKIPPED_SONGS]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun recordSkip(songId: Long) {
        dataStore.edit { preferences ->
            val current = preferences[KEY_SKIPPED_SONGS]?.toMutableSet() ?: mutableSetOf()
            current.add(songId.toString())
            preferences[KEY_SKIPPED_SONGS] = current
        }
    }

    /**
     * Map of song ID to play count.
     */
    val playCounts: Flow<Map<Long, Int>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val jsonStr = preferences[KEY_PLAY_COUNTS] ?: return@map emptyMap()
            try {
                val obj = JSONObject(jsonStr)
                val map = mutableMapOf<Long, Int>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k.toLong()] = obj.getInt(k)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun recordPlay(songId: Long) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_PLAY_COUNTS]
            val obj = if (currentJson != null) {
                try { JSONObject(currentJson) } catch (_: Exception) { JSONObject() }
            } else JSONObject()

            val key = songId.toString()
            val currentCount = obj.optInt(key, 0)
            obj.put(key, currentCount + 1)
            preferences[KEY_PLAY_COUNTS] = obj.toString()
        }
    }

    suspend fun saveSearchQuery(query: String) = addSearchQuery(query)
    suspend fun recordSongSkipped(songId: Long) = recordSkip(songId)
    suspend fun recordSongPlayed(songId: Long) = recordPlay(songId)

    val userName: Flow<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_USER_NAME] ?: "Muhil"
        }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name.trim().ifBlank { "Muhil" }
        }
    }

    val profilePictureUri: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_PROFILE_PICTURE_URI]
        }

    suspend fun setProfilePictureUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(KEY_PROFILE_PICTURE_URI)
            } else {
                preferences[KEY_PROFILE_PICTURE_URI] = uri.trim()
            }
        }
    }
}
