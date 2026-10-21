package com.muhil.music.data.remote

import com.muhil.music.domain.model.Album
import com.muhil.music.domain.model.Artist
import com.muhil.music.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiusMusicDataSource @Inject constructor() {

    private val appName = "MuhilMusic"
    private val userAgent = "MuhilMusic/1.0 (Android)"
    private val baseUrl = "https://api.audius.co"
    private val connectTimeoutMs = 12000
    private val readTimeoutMs = 15000

    /**
     * Search full-length tracks on Audius.
     */
    suspend fun searchSongs(query: String, limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "$baseUrl/v1/tracks/search?query=$encoded&limit=$limit&app_name=$appName"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseTracks(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching Audius tracks for: $query")
            emptyList()
        }
    }

    /**
     * Search artists / creators on Audius.
     */
    suspend fun searchArtists(query: String, limit: Int = 15): List<Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "$baseUrl/v1/users/search?query=$encoded&limit=$limit&app_name=$appName"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseUsers(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching Audius artists for: $query")
            emptyList()
        }
    }

    /**
     * Search playlists on Audius (mapped to Album domain model).
     */
    suspend fun searchPlaylists(query: String, limit: Int = 15): List<Album> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "$baseUrl/v1/playlists/search?query=$encoded&limit=$limit&app_name=$appName"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parsePlaylists(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching Audius playlists for: $query")
            emptyList()
        }
    }

    /**
     * Get trending full-length tracks from Audius.
     */
    suspend fun getTrendingTracks(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val endpoint = "$baseUrl/v1/tracks/trending?limit=$limit&app_name=$appName"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseTracks(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Audius trending tracks")
            emptyList()
        }
    }

    /**
     * Get trending tracks by genre from Audius.
     */
    suspend fun getTrendingTracksByGenre(genre: String, limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        val mappedGenre = mapGenre(genre)
        val endpoint = if (mappedGenre.isNotBlank()) {
            val encoded = URLEncoder.encode(mappedGenre, "UTF-8")
            "$baseUrl/v1/tracks/trending?genre=$encoded&limit=$limit&app_name=$appName"
        } else {
            "$baseUrl/v1/tracks/trending?limit=$limit&app_name=$appName"
        }
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseTracks(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Audius genre tracks: $genre")
            emptyList()
        }
    }

    /**
     * Map app genre chip labels to supported Audius genre names.
     */
    fun mapGenre(genre: String): String = when (genre.lowercase()) {
        "all" -> ""
        "pop" -> "Pop"
        "electronic" -> "Electronic"
        "hip-hop", "hip hop", "rap" -> "Hip-Hop/Rap"
        "rock" -> "Rock"
        "r&b" -> "R&B/Soul"
        "classical" -> "Classical"
        "indie" -> "Indie"
        "ambient" -> "Ambient"
        "latin" -> "Latin"
        "jazz" -> "Jazz"
        "metal" -> "Metal"
        "reggae" -> "Reggae"
        else -> genre
    }

    /**
     * Resolve direct stream URL by following the 302 redirect or returning the official stream endpoint.
     */
    suspend fun resolveStreamUrl(trackId: String): String = withContext(Dispatchers.IO) {
        val streamUrl = "$baseUrl/v1/tracks/$trackId/stream?app_name=$appName"
        try {
            val conn = URL(streamUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "HEAD"
            conn.setRequestProperty("User-Agent", userAgent)
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                if (!location.isNullOrBlank()) {
                    return@withContext location
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not pre-resolve Audius stream redirect for $trackId")
        }
        streamUrl
    }

    /**
     * Deterministic, collision-resistant 64-bit ID generator from Audius string ID.
     */
    fun audiusIdToLong(audiusId: String): Long {
        var h = 1125899906842597L
        for (c in audiusId) {
            h = 31L * h + c.code.toLong()
        }
        // Mask to positive and high-bit offset to make Audius IDs unique and distinguishable from local/iTunes
        return (h and 0x3FFFFFFFFFFFFFFFL) or 0x4000000000000000L
    }

    fun parseTracks(jsonStr: String): List<Song> {
        val root = JSONObject(jsonStr)
        val dataArray = root.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<Song>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue

            // Respect track access restrictions and availability
            val isStreamable = item.optBoolean("is_streamable", true)
            if (!isStreamable) continue

            val isDeleted = item.optBoolean("is_delete", false)
            if (isDeleted) continue

            val trackId = item.optString("id", "")
            if (trackId.isBlank()) continue

            val title = item.optString("title", "Unknown Track").trim()
            val userObj = item.optJSONObject("user")
            val artistName = userObj?.optString("name", "Unknown Artist")?.trim() ?: "Unknown Artist"
            val genre = item.optString("genre", "Music")

            // Real full duration in seconds converted to ms
            val durationSec = item.optLong("duration", 0L)
            val durationMs = if (durationSec > 0) durationSec * 1000L else 180000L

            // Extract high-res artwork
            val artworkObj = item.optJSONObject("artwork")
            val artUri = artworkObj?.optString("480x480", null)?.takeIf { it.isNotBlank() }
                ?: artworkObj?.optString("1000x1000", null)?.takeIf { it.isNotBlank() }
                ?: artworkObj?.optString("150x150", null)?.takeIf { it.isNotBlank() }

            val streamUri = "$baseUrl/v1/tracks/$trackId/stream?app_name=$appName"

            list.add(
                Song(
                    id              = audiusIdToLong(trackId),
                    title           = title,
                    artist          = artistName,
                    album           = if (genre.isNotBlank()) "$genre • Audius" else "Audius Full Track",
                    duration        = durationMs,
                    uri             = streamUri,
                    albumArtUri     = artUri,
                    isOnline        = true,
                    isPreview       = false, // Authorized Full Track!
                    genre           = genre,
                    provider        = Song.PROVIDER_AUDIUS,
                    providerTrackId = trackId
                )
            )
        }
        return list
    }

    fun parseUsers(jsonStr: String): List<Artist> {
        val root = JSONObject(jsonStr)
        val dataArray = root.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<Artist>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue
            val userId = item.optString("id", "")
            val name = item.optString("name", "").trim()
            if (userId.isBlank() || name.isBlank()) continue

            val trackCount = item.optInt("track_count", 0)
            val albumCount = item.optInt("album_count", 0)

            val picObj = item.optJSONObject("profile_picture")
            val artUri = picObj?.optString("480x480", null)?.takeIf { it.isNotBlank() }
                ?: picObj?.optString("1000x1000", null)?.takeIf { it.isNotBlank() }
                ?: picObj?.optString("150x150", null)?.takeIf { it.isNotBlank() }

            list.add(
                Artist(
                    id         = audiusIdToLong(userId),
                    name       = name,
                    songCount  = trackCount,
                    albumCount = albumCount,
                    artUri     = artUri,
                    isOnline   = true,
                    provider   = Song.PROVIDER_AUDIUS,
                    providerId = userId
                )
            )
        }
        return list
    }

    fun parsePlaylists(jsonStr: String): List<Album> {
        val root = JSONObject(jsonStr)
        val dataArray = root.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<Album>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue
            val playlistId = item.optString("id", "")
            val title = item.optString("playlist_name", "").trim()
            if (playlistId.isBlank() || title.isBlank()) continue

            val userObj = item.optJSONObject("user")
            val artistName = userObj?.optString("name", "Audius Creator")?.trim() ?: "Audius Creator"
            val trackCount = item.optInt("track_count", 0)

            val artworkObj = item.optJSONObject("artwork")
            val artUri = artworkObj?.optString("480x480", null)?.takeIf { it.isNotBlank() }
                ?: artworkObj?.optString("1000x1000", null)?.takeIf { it.isNotBlank() }
                ?: artworkObj?.optString("150x150", null)?.takeIf { it.isNotBlank() }

            list.add(
                Album(
                    id         = audiusIdToLong(playlistId),
                    title      = title,
                    artist     = artistName,
                    artUri     = artUri,
                    songCount  = trackCount,
                    isOnline   = true,
                    provider   = Song.PROVIDER_AUDIUS,
                    providerId = playlistId
                )
            )
        }
        return list
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }
            } else {
                Timber.w("Audius API HTTP $responseCode for $urlStr")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Audius HTTP request failed: $urlStr")
            null
        } finally {
            conn?.disconnect()
        }
    }
}
