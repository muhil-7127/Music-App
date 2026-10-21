package com.muhil.music.data.remote

import com.muhil.music.domain.model.LyricLine
import com.muhil.music.domain.model.LyricsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
class OnlineLyricsDataSource @Inject constructor() {

    private val userAgent = "MuhilMusic/1.0 (Android)"
    private val connectTimeoutMs = 10000
    private val readTimeoutMs = 12000

    /**
     * Fetch synchronized or plain lyrics from LRCLIB.
     */
    suspend fun fetchLyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L
    ): LyricsState = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext LyricsState.Unavailable

        val cleanTitle = sanitize(title)
        val cleanArtist = sanitize(artist)
        val durationSec = (durationMs / 1000L).toInt()

        // 1. Try exact get endpoint if duration is available
        if (durationSec > 10) {
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle&duration=$durationSec"
            val response = httpGet(url)
            if (response != null) {
                val state = parseLyricsObject(response)
                if (state !is LyricsState.Unavailable) return@withContext state
            }
        }

        // 2. Try search endpoint
        val query = "$cleanArtist $cleanTitle".trim()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://lrclib.net/api/search?q=$encodedQuery"
        val searchResponse = httpGet(searchUrl)
        if (searchResponse != null) {
            val state = parseSearchArray(searchResponse)
            if (state !is LyricsState.Unavailable) return@withContext state
        }

        // 3. Fallback: search by title only
        val titleOnlyUrl = "https://lrclib.net/api/search?q=${URLEncoder.encode(cleanTitle, "UTF-8")}"
        val titleOnlyResponse = httpGet(titleOnlyUrl)
        if (titleOnlyResponse != null) {
            val state = parseSearchArray(titleOnlyResponse)
            if (state !is LyricsState.Unavailable) return@withContext state
        }

        LyricsState.Unavailable
    }

    private fun parseLyricsObject(jsonStr: String): LyricsState {
        return try {
            val obj = JSONObject(jsonStr)
            val synced = obj.optString("syncedLyrics", "").trim()
            val plain = obj.optString("plainLyrics", "").trim()

            when {
                synced.isNotBlank() -> {
                    val lines = parseLrc(synced)
                    if (lines.isNotEmpty()) LyricsState.Synced(lines) else LyricsState.Plain(synced)
                }
                plain.isNotBlank() -> LyricsState.Plain(plain)
                else -> LyricsState.Unavailable
            }
        } catch (e: Exception) {
            LyricsState.Unavailable
        }
    }

    private fun parseSearchArray(jsonStr: String): LyricsState {
        return try {
            val array = JSONArray(jsonStr)
            if (array.length() == 0) return LyricsState.Unavailable

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val synced = item.optString("syncedLyrics", "").trim()
                val plain = item.optString("plainLyrics", "").trim()

                if (synced.isNotBlank()) {
                    val lines = parseLrc(synced)
                    if (lines.isNotEmpty()) return LyricsState.Synced(lines)
                }
                if (plain.isNotBlank()) {
                    return LyricsState.Plain(plain)
                }
            }
            LyricsState.Unavailable
        } catch (e: Exception) {
            LyricsState.Unavailable
        }
    }

    /**
     * Parse standard LRC format [mm:ss.xx] line
     */
    fun parseLrc(lrc: String): List<LyricLine> {
        val pattern = Regex("\\[(\\d+):(\\d+\\.\\d+)](.+)")
        return lrc.lines()
            .mapNotNull { line ->
                pattern.find(line.trim())?.let { match ->
                    val minutes = match.groupValues[1].toLong()
                    val seconds = match.groupValues[2].toDouble()
                    val text = match.groupValues[3].trim()
                    val timestampMs = (minutes * 60 * 1000 + seconds * 1000).toLong()
                    if (text.isNotBlank()) LyricLine(timestampMs, text) else null
                }
            }
            .sortedBy { it.timestampMs }
    }

    fun sanitize(input: String): String {
        return input
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("\\b(feat\\.|ft\\.|featuring)\\b.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*(Deluxe Edition|Deluxe|Remastered|Remaster|Bonus Track|Bonus|Live|Single|EP|Radio Edit).*$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun httpGet(urlString: String): String? {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", userAgent)
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = connectTimeoutMs
        conn.readTimeout = readTimeoutMs
        conn.instanceFollowRedirects = true

        return try {
            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val sb = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w("HTTP request failed: $urlString - ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }
}
