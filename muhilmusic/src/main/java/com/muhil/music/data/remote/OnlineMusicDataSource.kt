package com.muhil.music.data.remote

import com.muhil.music.domain.model.Album
import com.muhil.music.domain.model.Artist
import com.muhil.music.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
class OnlineMusicDataSource @Inject constructor() {

    private val userAgent = "MuhilMusic/1.0 (Android)"
    private val connectTimeoutMs = 7000
    private val readTimeoutMs = 8000

    /**
     * Search songs by title, artist, or album on the web (iTunes Search API).
     */
    suspend fun searchSongs(query: String, limit: Int = 40, country: String = "IN"): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=$limit&country=$country"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseSongs(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching online songs for query: $query")
            emptyList()
        }
    }

    /**
     * Search artists by name on the web.
     */
    suspend fun searchArtists(query: String, limit: Int = 15, country: String = "IN"): List<Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "https://itunes.apple.com/search?term=$encoded&entity=musicArtist&limit=$limit&country=$country"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseArtists(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching artists: $query")
            emptyList()
        }
    }

    /**
     * Search albums by name on the web.
     */
    suspend fun searchAlbums(query: String, limit: Int = 20, country: String = "IN"): List<Album> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val endpoint = "https://itunes.apple.com/search?term=$encoded&entity=album&limit=$limit&country=$country"
        try {
            val jsonStr = httpGet(endpoint) ?: return@withContext emptyList()
            parseAlbums(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Error searching albums: $query")
            emptyList()
        }
    }

    /**
     * Discover featured/trending songs across top global charts.
     */
    suspend fun getFeaturedSongs(): List<Song> = withContext(Dispatchers.IO) {
        val trendingQueries = listOf("Top Hits 2024", "Taylor Swift", "The Weeknd", "Coldplay", "A.R. Rahman")
        coroutineScope {
            val deferreds = trendingQueries.map { term ->
                async { searchSongs(term, limit = 6) }
            }
            val allResults = mutableListOf<Song>()
            val seenIds = mutableSetOf<Long>()
            for (d in deferreds) {
                val list = try { d.await() } catch (e: Exception) { emptyList() }
                for (song in list) {
                    if (seenIds.add(song.id)) {
                        allResults.add(song)
                    }
                }
            }
            allResults
        }
    }

    /**
     * Discover songs by genre or mood (Pop, Rock, Hip-Hop, R&B, Electronic, Tamil, Bollywood, Classical).
     */
    suspend fun getGenreSongs(genre: String, limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        val searchTerm = when (genre.lowercase()) {
            "all" -> "Top Hits"
            "pop" -> "Pop Hits"
            "rock" -> "Rock Classics"
            "hip-hop" -> "Hip Hop Hits"
            "r&b" -> "R&B Soul"
            "electronic" -> "Electronic Dance"
            "tamil" -> "Tamil Hits"
            "bollywood" -> "Bollywood Hits"
            "classical" -> "Classical Piano"
            "indie" -> "Indie Pop"
            else -> "$genre Music"
        }
        searchSongs(searchTerm, limit = limit)
    }

    /**
     * Get popular artists list with rich metadata.
     */
    suspend fun getTopArtists(): List<Artist> = withContext(Dispatchers.IO) {
        listOf(
            Artist(id = 89445L, name = "Coldplay", songCount = 42, albumCount = 9, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/f5/93/8c/f5938c49-964c-31d1-4b33-78b634f71fb7/190295978075.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 479756766L, name = "The Weeknd", songCount = 56, albumCount = 6, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/91/92/ff/9192ffcf-51de-733c-3df9-94ae6d8d0d54/15UMGIM60867.rgb.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 159260351L, name = "Taylor Swift", songCount = 88, albumCount = 11, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/8e/3c/6e/8e3c6e93-6b4d-ef34-11e2-b7e64cc90b8f/23UMGIM80786.rgb.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 49669528L, name = "A.R. Rahman", songCount = 65, albumCount = 20, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/c9/a1/38/c9a13809-53e7-2b36-efd3-90d56569165b/886443685608.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 1450330588L, name = "Billie Eilish", songCount = 38, albumCount = 3, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/30/1a/10/301a100f-d5cf-4df5-29f9-67990be1030e/24UMGIM35222.rgb.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 543888365L, name = "Anirudh Ravichander", songCount = 50, albumCount = 15, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/d1/aa/22/d1aa223d-249d-6490-25fc-8ea736dd6313/190296767357.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 368183298L, name = "Ed Sheeran", songCount = 45, albumCount = 7, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/7c/e8/62/7ce862a9-7c85-231a-6a58-aa03b7ec3b2d/190296683886.jpg/600x600bb.jpg", isOnline = true),
            Artist(id = 1031397873L, name = "Dua Lipa", songCount = 34, albumCount = 3, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/6b/64/00/6b6400c4-95c5-5558-8aa3-5240c5f2129e/5054197992790.jpg/600x600bb.jpg", isOnline = true)
        )
    }

    /**
     * Get trending albums.
     */
    suspend fun getTrendingAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = searchAlbums("Greatest Hits 2024", limit = 12)
        if (albums.isNotEmpty()) albums else listOf(
            Album(id = 1440857781L, title = "Parachutes", artist = "Coldplay", songCount = 10, year = 2000, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/f5/93/8c/f5938c49-964c-31d1-4b33-78b634f71fb7/190295978075.jpg/600x600bb.jpg", isOnline = true),
            Album(id = 1440828211L, title = "Starboy", artist = "The Weeknd", songCount = 18, year = 2016, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/91/92/ff/9192ffcf-51de-733c-3df9-94ae6d8d0d54/15UMGIM60867.rgb.jpg/600x600bb.jpg", isOnline = true),
            Album(id = 1440935467L, title = "1989 (Taylor's Version)", artist = "Taylor Swift", songCount = 21, year = 2023, artUri = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/8e/3c/6e/8e3c6e93-6b4d-ef34-11e2-b7e64cc90b8f/23UMGIM80786.rgb.jpg/600x600bb.jpg", isOnline = true)
        )
    }

    private fun parseSongs(jsonStr: String): List<Song> {
        val root = JSONObject(jsonStr)
        val count = root.optInt("resultCount", 0)
        if (count == 0) return emptyList()
        val array = root.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<Song>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val wrapperType = item.optString("wrapperType", "")
            if (wrapperType.isNotEmpty() && wrapperType != "track") continue

            val previewUrl = item.optString("previewUrl", "")
            if (previewUrl.isBlank()) continue

            val trackId = item.optLong("trackId", 0L)
            if (trackId == 0L) continue

            val trackName = item.optString("trackName", "Unknown Track")
            val artistName = item.optString("artistName", "Unknown Artist")
            val collectionName = item.optString("collectionName", "Single")
            val trackTimeMillis = item.optLong("trackTimeMillis", 30000L)
            val artworkRaw = item.optString("artworkUrl100", "")
            val artworkHighRes = if (artworkRaw.isNotBlank()) {
                artworkRaw.replace("100x100bb", "600x600bb")
            } else null
            val genre = item.optString("primaryGenreName", "Pop")

            list.add(
                Song(
                    id              = trackId,
                    title           = trackName,
                    artist          = artistName,
                    album           = collectionName,
                    duration        = trackTimeMillis,
                    uri             = previewUrl,
                    albumArtUri     = artworkHighRes,
                    isOnline        = true,
                    isPreview       = true,
                    genre           = genre,
                    provider        = Song.PROVIDER_ITUNES,
                    providerTrackId = trackId.toString()
                )
            )
        }
        return list
    }

    private fun parseArtists(jsonStr: String): List<Artist> {
        val root = JSONObject(jsonStr)
        val array = root.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<Artist>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val artistId = item.optLong("artistId", 0L)
            val artistName = item.optString("artistName", "")
            if (artistId == 0L || artistName.isBlank()) continue

            list.add(
                Artist(
                    id         = artistId,
                    name       = artistName,
                    songCount  = 0,
                    isOnline   = true,
                    provider   = Song.PROVIDER_ITUNES,
                    providerId = artistId.toString()
                )
            )
        }
        return list
    }

    private fun parseAlbums(jsonStr: String): List<Album> {
        val root = JSONObject(jsonStr)
        val array = root.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<Album>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val collectionId = item.optLong("collectionId", 0L)
            val collectionName = item.optString("collectionName", "")
            val artistName = item.optString("artistName", "Unknown Artist")
            if (collectionId == 0L || collectionName.isBlank()) continue

            val artworkRaw = item.optString("artworkUrl100", "")
            val artworkHighRes = if (artworkRaw.isNotBlank()) {
                artworkRaw.replace("100x100bb", "600x600bb")
            } else null
            val trackCount = item.optInt("trackCount", 0)

            list.add(
                Album(
                    id         = collectionId,
                    title      = collectionName,
                    artist     = artistName,
                    artUri     = artworkHighRes,
                    songCount  = trackCount,
                    isOnline   = true,
                    provider   = Song.PROVIDER_ITUNES,
                    providerId = collectionId.toString()
                )
            )
        }
        return list
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
                Timber.w("HTTP error code $code from $urlString")
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}
