package com.muhil.music.data.remote

import android.util.Base64
import com.muhil.music.domain.model.Album
import com.muhil.music.domain.model.Artist
import com.muhil.music.domain.model.Song
import com.muhil.music.domain.search.MovieSoundtrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JioSaavnMusicDataSource @Inject constructor() {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val connectTimeoutMs = 8000
    private val readTimeoutMs = 9000
    private val baseUrl = "https://www.jiosaavn.com/api.php"

    /**
     * Search genuine full-length songs with decryptable direct streaming audio.
     */
    suspend fun searchSongs(query: String, limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "$baseUrl?__call=search.getResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encoded&p=1&n=$limit"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseSongs(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching Saavn songs for: $query")
            emptyList()
        }
    }

    /**
     * Search movie and music albums.
     */
    suspend fun searchAlbums(query: String, limit: Int = 15): List<Album> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "$baseUrl?__call=search.getAlbumResults&_format=json&_marker=0&cc=in&includeMetaTags=1&q=$encoded&p=1&n=$limit"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseAlbums(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching Saavn albums for: $query")
            emptyList()
        }
    }

    /**
     * Fetch complete soundtrack album tracks as full songs.
     */
    suspend fun getAlbumDetails(albumId: String): List<Song> = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext emptyList()
        val endpoint = "$baseUrl?__call=content.getAlbumDetails&_format=json&_marker=0&cc=in&includeMetaTags=1&albumid=$albumId"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseAlbumSongs(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching album details for ID: $albumId")
            emptyList()
        }
    }

    /**
     * Fetch trending Tamil hits as 100% full songs.
     */
    suspend fun getTamilHits(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        searchSongs("Tamil Top Hits 2024", limit)
    }

    /**
     * Fetch soft romantic Tamil melodies as full songs.
     */
    suspend fun getTamilMelodies(limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        searchSongs("Tamil Romantic Melodies", limit)
    }

    /**
     * Fetch evergreen Tamil classics as full songs.
     */
    suspend fun getTamilClassics(limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        searchSongs("Evergreen Tamil Ilaiyaraaja SPB Classics", limit)
    }

    /**
     * Fetch top Tamil film movie soundtracks with all their full tracks.
     */
    suspend fun getTamilSoundtracks(limit: Int = 8): List<MovieSoundtrack> = withContext(Dispatchers.IO) {
        val movies = listOf("Leo", "Jailer", "Vikram", "Amaran", "Master", "Beast", "Thunivu", "Kaithi")
        val result = mutableListOf<MovieSoundtrack>()
        for (m in movies.take(limit)) {
            val songs = searchSongs(m, limit = 6)
            val fullTracks = songs.filter { !it.isPreview && it.uri.isNotBlank() }
            if (fullTracks.size >= 2) {
                val composer = fullTracks.firstOrNull()?.artist ?: "Kollywood Composer"
                val art = fullTracks.firstOrNull()?.albumArtUri
                result.add(
                    MovieSoundtrack(
                        movieName = m,
                        composer = composer,
                        albumArtUri = art,
                        tracks = fullTracks
                    )
                )
            }
        }
        result
    }

    // ==========================================
    // JSON Parsers
    // ==========================================

    fun parseSongs(jsonStr: String): List<Song> {
        val root = try { JSONObject(jsonStr) } catch (e: Exception) { return emptyList() }
        val resultsArray = root.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<Song>()

        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.optJSONObject(i) ?: continue
            val song = parseSongObject(item)
            if (song != null) list.add(song)
        }
        return list
    }

    fun parseAlbumSongs(jsonStr: String): List<Song> {
        val root = try { JSONObject(jsonStr) } catch (e: Exception) { return emptyList() }
        val songsArray = root.optJSONArray("songs") ?: return emptyList()
        val list = mutableListOf<Song>()

        for (i in 0 until songsArray.length()) {
            val item = songsArray.optJSONObject(i) ?: continue
            val song = parseSongObject(item)
            if (song != null) list.add(song)
        }
        return list
    }

    fun parseAlbums(jsonStr: String): List<Album> {
        val root = try { JSONObject(jsonStr) } catch (e: Exception) { return emptyList() }
        val resultsArray = root.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<Album>()

        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.optJSONObject(i) ?: continue
            val albumId = item.optString("albumid", item.optString("id", "")).trim()
            val rawTitle = item.optString("title", "").trim()
            if (albumId.isBlank() || rawTitle.isBlank()) continue

            val title = unescapeHtml(rawTitle)
            val artist = unescapeHtml(item.optString("music", item.optString("artist", "Kollywood")))
            val year = item.optString("year", "").toIntOrNull() ?: item.optInt("year", 0)
            val songCount = item.optString("song_count", "").toIntOrNull() ?: item.optInt("song_count", 0)
            val artUri = resolveArtworkUrl(item.optString("image", ""))

            list.add(
                Album(
                    id = saavnIdToLong(albumId),
                    title = title,
                    artist = artist,
                    artUri = artUri,
                    songCount = songCount,
                    year = year,
                    isOnline = true,
                    provider = Song.PROVIDER_SAAVN,
                    providerId = albumId
                )
            )
        }
        return list
    }

    private fun parseSongObject(item: JSONObject): Song? {
        val songIdStr = item.optString("id", "").trim()
        val rawTitle = item.optString("song", item.optString("title", "")).trim()
        if (songIdStr.isBlank() || rawTitle.isBlank()) return null

        val encryptedMediaUrl = item.optString("encrypted_media_url", "")
        if (encryptedMediaUrl.isBlank()) return null

        val streamUrl = decryptMediaUrl(encryptedMediaUrl) ?: return null
        val title = unescapeHtml(rawTitle)
        val artist = unescapeHtml(item.optString("singers", item.optString("music", "Artist")))
        val album = unescapeHtml(item.optString("album", ""))
        val rawDuration = item.optLong("duration", 0L)
        val durationMs = if (rawDuration > 0) rawDuration * 1000L else 240000L
        val artUri = resolveArtworkUrl(item.optString("image", ""))

        return Song(
            id = saavnIdToLong(songIdStr),
            title = title,
            artist = artist,
            album = album,
            duration = durationMs,
            uri = streamUrl,
            albumArtUri = artUri,
            isOnline = true,
            isPreview = false, // 100% Full Song!
            provider = Song.PROVIDER_SAAVN,
            providerTrackId = songIdStr
        )
    }

    // ==========================================
    // Media URL Decryption (DES/ECB/PKCS5Padding)
    // ==========================================

    fun decodeBase64(input: String): ByteArray {
        val clean = input.trim()
        return try {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(clean)
        }
    }

    fun decryptMediaUrl(encrypted: String): String? {
        if (encrypted.isBlank()) return null
        return try {
            val keyBytes = "38346591".toByteArray(Charsets.UTF_8)
            val key = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decBytes = cipher.doFinal(decodeBase64(encrypted))
            val url = String(decBytes, Charsets.UTF_8).trim()
            if (url.endsWith("_96.mp4")) {
                url.replace("_96.mp4", "_160.mp4")
            } else if (url.endsWith("_96.m4a")) {
                url.replace("_96.m4a", "_160.m4a")
            } else {
                url
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to decrypt Saavn media URL")
            null
        }
    }

    fun resolveArtworkUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        return rawUrl.replace("150x150", "500x500")
            .replace("50x50", "500x500")
    }

    fun unescapeHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()
    }

    fun saavnIdToLong(idStr: String): Long {
        val numeric = idStr.toLongOrNull()
        return if (numeric != null && numeric > 0) {
            500_000_000L + (numeric % 100_000_000L)
        } else {
            val hash = idStr.hashCode().toLong()
            500_000_000L + (if (hash < 0) -hash else hash) % 100_000_000L
        }
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
                Timber.w("Saavn API HTTP $responseCode for $urlStr")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Saavn HTTP request failed: $urlStr")
            null
        } finally {
            conn?.disconnect()
        }
    }
}
