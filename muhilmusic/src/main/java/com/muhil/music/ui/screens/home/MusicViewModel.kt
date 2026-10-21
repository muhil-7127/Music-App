package com.muhil.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muhil.music.data.local.entity.RecentlyPlayedEntity
import com.muhil.music.data.repository.MusicRepository
import com.muhil.music.domain.model.*
import com.muhil.music.domain.search.MovieSoundtrack
import com.muhil.music.domain.search.QueryIntent
import com.muhil.music.domain.search.RankedSearchResult
import com.muhil.music.media.MuhilPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MusicUiState(
    val featuredSongs: List<Song> = emptyList(),
    val genreSongs: List<Song> = emptyList(),
    val selectedGenre: String = "All",
    val topArtists: List<Artist> = emptyList(),
    val trendingAlbums: List<Album> = emptyList(),
    val recommendations: List<Song> = emptyList(),
    val recommendationSeed: Song? = null,
    val isRecommendationsLoading: Boolean = false,
    val localSongs: List<Song> = emptyList(),
    val localAlbums: List<Album> = emptyList(),
    val localArtists: List<Artist> = emptyList(),
    val recentlyPlayed: List<RecentlyPlayedEntity> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val favoriteSongs: List<Song> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val searchTopMatch: Song? = null,
    val searchMovieSoundtrack: MovieSoundtrack? = null,
    val searchSoundtracks: List<MovieSoundtrack> = emptyList(),
    val searchIntent: QueryIntent = QueryIntent.GENERAL,
    val searchArtists: List<Artist> = emptyList(),
    val searchAlbums: List<Album> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val preferredLanguage: String = "Tamil",
    val tamilHits: List<Song> = emptyList(),
    val tamilSoundtracks: List<MovieSoundtrack> = emptyList(),
    val tamilClassics: List<Song> = emptyList(),
    val tamilMelodies: List<Song> = emptyList(),
    val tamilArtists: List<Artist> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false
) {
    val allSongs: List<Song> get() = featuredSongs.ifEmpty { localSongs }
    val albums: List<Album> get() = trendingAlbums.ifEmpty { localAlbums }
    val artists: List<Artist> get() = topArtists.ifEmpty { localArtists }
}

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    val playerController: MuhilPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    private var searchJob: Job? = null
    private var recommendationsJob: Job? = null

    init {
        observeFavorites()
        observeRecentlyPlayed()
        observePreferences()
        observePlaybackForRecommendations()
        loadHomeContent()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.preferredLanguage.collect { lang ->
                _uiState.update { it.copy(preferredLanguage = lang) }
            }
        }
        viewModelScope.launch {
            repository.searchHistory.collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteSongIds().collect { ids ->
                _uiState.update { it.copy(favoriteSongIds = ids.toSet()) }
            }
        }
        viewModelScope.launch {
            repository.getFavoriteSongs().collect { songs ->
                _uiState.update { it.copy(favoriteSongs = songs) }
            }
        }
    }

    private fun observeRecentlyPlayed() {
        viewModelScope.launch {
            repository.getRecentlyPlayed().collect { recent ->
                _uiState.update { it.copy(recentlyPlayed = recent) }
            }
        }
    }

    private fun observePlaybackForRecommendations() {
        viewModelScope.launch {
            playerController.playbackState
                .map { it.currentSong }
                .distinctUntilChanged()
                .collect { song ->
                    if (song != null) {
                        loadRecommendations(song)
                    }
                }
        }
    }

    /**
     * Primary online music discovery: loads featured songs, top artists, trending albums,
     * and Tamil-focused discovery categories concurrently.
     */
    fun loadHomeContent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                coroutineScope {
                    val featuredDef = async { repository.getFeaturedSongs(forceRefresh) }
                    val artistsDef = async { repository.getTopArtists(forceRefresh) }
                    val albumsDef = async { repository.getTrendingAlbums(forceRefresh) }
                    val tamilHitsDef = async { repository.getTamilHits(forceRefresh) }
                    val tamilSoundtracksDef = async { repository.getTamilSoundtracks(forceRefresh) }
                    val tamilClassicsDef = async { repository.getTamilClassics(forceRefresh) }
                    val tamilMelodiesDef = async { repository.getTamilMelodies(forceRefresh) }
                    val tamilArtistsDef = async { repository.getTamilArtists(forceRefresh) }

                    val featured = featuredDef.await()
                    val artists = artistsDef.await()
                    val albums = albumsDef.await()
                    val tamilHits = tamilHitsDef.await()
                    val tamilSoundtracks = tamilSoundtracksDef.await()
                    val tamilClassics = tamilClassicsDef.await()
                    val tamilMelodies = tamilMelodiesDef.await()
                    val tamilArtists = tamilArtistsDef.await()

                    _uiState.update {
                        it.copy(
                            featuredSongs = featured,
                            topArtists = artists,
                            trendingAlbums = albums,
                            tamilHits = tamilHits,
                            tamilSoundtracks = tamilSoundtracks,
                            tamilClassics = tamilClassics,
                            tamilMelodies = tamilMelodies,
                            tamilArtists = tamilArtists,
                            isLoading = false,
                            error = if (featured.isEmpty() && it.localSongs.isEmpty() && tamilHits.isEmpty()) "Unable to reach music servers. Tap retry." else null
                        )
                    }

                    // Seed recommendations with top track if no seed exists yet
                    if (_uiState.value.recommendationSeed == null) {
                        val seed = tamilHits.firstOrNull() ?: featured.firstOrNull()
                        if (seed != null) {
                            loadRecommendations(seed)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading online home content")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (it.featuredSongs.isEmpty() && it.localSongs.isEmpty() && it.tamilHits.isEmpty()) "Failed to load online music. Check connection." else null
                    )
                }
            }
        }
    }

    /**
     * Load intelligent recommendations based on a seed song.
     */
    fun loadRecommendations(seed: Song) {
        if (_uiState.value.recommendationSeed?.id == seed.id && _uiState.value.recommendations.isNotEmpty()) return

        recommendationsJob?.cancel()
        recommendationsJob = viewModelScope.launch {
            _uiState.update { it.copy(isRecommendationsLoading = true, recommendationSeed = seed) }
            try {
                val recs = repository.getRecommendations(seed, limit = 15)
                _uiState.update {
                    it.copy(
                        recommendations = recs,
                        isRecommendationsLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading recommendations for ${seed.title}")
                _uiState.update { it.copy(isRecommendationsLoading = false) }
            }
        }
    }

    /**
     * Select genre chip: queries online songs by genre/mood with in-memory caching.
     */
    fun selectGenre(genre: String) {
        if (_uiState.value.selectedGenre == genre && _uiState.value.genreSongs.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedGenre = genre, isLoading = true) }
            try {
                val songs = repository.getGenreSongs(genre)
                _uiState.update { it.copy(genreSongs = songs, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching genre songs: $genre")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Real-time music search with intent detection, exact-match ranking, and Tamil awareness.
     */
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchTopMatch = null,
                    searchMovieSoundtrack = null,
                    searchSoundtracks = emptyList(),
                    searchIntent = QueryIntent.GENERAL,
                    searchArtists = emptyList(),
                    searchAlbums = emptyList(),
                    isSearching = false,
                    searchError = null
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            val trimmedQuery = query.trim()
            repository.saveSearchQuery(trimmedQuery)
            try {
                coroutineScope {
                    val rankedDef = async { repository.searchOnlineRanked(trimmedQuery, preferredLanguage = _uiState.value.preferredLanguage) }
                    val artistsDef = async { repository.searchOnlineArtists(trimmedQuery) }
                    val albumsDef = async { repository.searchOnlineAlbums(trimmedQuery) }

                    val ranked = rankedDef.await()
                    val artists = artistsDef.await()
                    val albums = albumsDef.await()

                    _uiState.update {
                        it.copy(
                            searchResults = ranked.rankedSongs,
                            searchTopMatch = ranked.topMatch,
                            searchMovieSoundtrack = ranked.movieSoundtrack,
                            searchSoundtracks = ranked.allSoundtracks,
                            searchIntent = ranked.intent,
                            searchArtists = artists,
                            searchAlbums = albums,
                            isSearching = false,
                            searchError = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during web search")
                _uiState.update { it.copy(isSearching = false, searchError = "Search failed. Check your connection.") }
            }
        }
    }

    /**
     * Secondary feature: scans local phone storage if user chooses to view local MP3s.
     */
    fun loadLocalLibrary(hasPermission: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(hasPermission = hasPermission) }
            if (hasPermission) {
                try {
                    val songs = repository.getLocalSongs()
                    val albums = repository.getLocalAlbums()
                    val artists = repository.getLocalArtists()
                    _uiState.update {
                        it.copy(
                            localSongs = songs,
                            localAlbums = albums,
                            localArtists = artists
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading local MP3 library")
                }
            }
        }
    }

    fun playSong(song: Song) {
        val currentQueue = if (song.isOnline) {
            _uiState.value.featuredSongs.ifEmpty {
                _uiState.value.searchResults.ifEmpty { listOf(song) }
            }
        } else {
            _uiState.value.localSongs.ifEmpty { listOf(song) }
        }

        val queue = if (currentQueue.any { it.id == song.id }) currentQueue else listOf(song) + currentQueue
        val idx = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerController.playSongs(queue, idx)
        viewModelScope.launch { repository.addToRecentlyPlayed(song) }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        playerController.playSongs(songs, startIndex)
        songs.getOrNull(startIndex)?.let { song ->
            viewModelScope.launch { repository.addToRecentlyPlayed(song) }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(ms: Long) = playerController.seekTo(ms)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleRepeat() = playerController.toggleRepeat()

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            if (song.id in _uiState.value.favoriteSongIds) {
                repository.removeFavorite(song.id)
            } else {
                repository.addFavorite(song)
            }
        }
    }

    fun toggleFavorite(songId: Long) {
        val song = _uiState.value.featuredSongs.find { it.id == songId }
            ?: _uiState.value.searchResults.find { it.id == songId }
            ?: _uiState.value.genreSongs.find { it.id == songId }
            ?: _uiState.value.localSongs.find { it.id == songId }
            ?: _uiState.value.favoriteSongs.find { it.id == songId }

        if (song != null) {
            toggleFavorite(song)
        } else {
            viewModelScope.launch {
                if (songId in _uiState.value.favoriteSongIds) {
                    repository.removeFavorite(songId)
                }
            }
        }
    }

    fun addToQueue(song: Song) {
        playerController.addToQueue(song)
    }

    fun addToQueue(songs: List<Song>) {
        playerController.addToQueue(songs)
    }

    fun getFavoriteSongs(): List<Song> = _uiState.value.favoriteSongs

    fun getRecentlyAddedSongs(limit: Int = 10): List<Song> =
        _uiState.value.featuredSongs.take(limit)

    fun setPreferredLanguage(lang: String) {
        viewModelScope.launch {
            repository.setPreferredLanguage(lang)
            loadHomeContent(forceRefresh = true)
        }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch {
            repository.removeSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun recordSongSkipped(songId: Long) {
        viewModelScope.launch {
            repository.recordSongSkipped(songId)
        }
    }

    fun recordSongPlayed(songId: Long) {
        viewModelScope.launch {
            repository.recordSongPlayed(songId)
        }
    }
}
