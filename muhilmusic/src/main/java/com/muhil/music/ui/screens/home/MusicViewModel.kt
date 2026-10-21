package com.muhil.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muhil.music.data.local.entity.RecentlyPlayedEntity
import com.muhil.music.data.repository.MusicRepository
import com.muhil.music.domain.model.*
import com.muhil.music.media.MuhilPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val localSongs: List<Song> = emptyList(),
    val localAlbums: List<Album> = emptyList(),
    val localArtists: List<Artist> = emptyList(),
    val recentlyPlayed: List<RecentlyPlayedEntity> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val favoriteSongs: List<Song> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val searchArtists: List<Artist> = emptyList(),
    val searchAlbums: List<Album> = emptyList(),
    val isSearching: Boolean = false,
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

    init {
        observeFavorites()
        observeRecentlyPlayed()
        loadHomeContent()
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

    /**
     * Primary online music discovery: loads featured songs, top artists, and trending albums from web.
     */
    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val featured = repository.getFeaturedSongs()
                val artists = repository.getTopArtists()
                val albums = repository.getTrendingAlbums()

                _uiState.update {
                    it.copy(
                        featuredSongs = featured,
                        topArtists = artists,
                        trendingAlbums = albums,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading online home content")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load online music. Check connection.") }
            }
        }
    }

    /**
     * Select genre chip: queries online songs by genre/mood.
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
     * Real-time web music search with debouncing.
     */
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchArtists = emptyList(),
                    searchAlbums = emptyList(),
                    isSearching = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val songs = repository.searchOnlineSongs(query)
                val artists = repository.searchOnlineArtists(query)
                val albums = repository.searchOnlineAlbums(query)

                _uiState.update {
                    it.copy(
                        searchResults = songs,
                        searchArtists = artists,
                        searchAlbums = albums,
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during web search")
                _uiState.update { it.copy(isSearching = false) }
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

    fun getFavoriteSongs(): List<Song> = _uiState.value.favoriteSongs

    fun getRecentlyAddedSongs(limit: Int = 10): List<Song> =
        _uiState.value.featuredSongs.take(limit)
}
