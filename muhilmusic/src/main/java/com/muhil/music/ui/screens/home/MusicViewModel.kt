package com.muhil.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muhil.music.data.local.entity.RecentlyPlayedEntity
import com.muhil.music.data.repository.MusicRepository
import com.muhil.music.domain.model.*
import com.muhil.music.media.MuhilPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicUiState(
    val allSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val recentlyPlayed: List<RecentlyPlayedEntity> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val searchResults: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    val playerController: MuhilPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    init {
        observeFavorites()
        observeRecentlyPlayed()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteSongIds().collect { ids ->
                _uiState.update { it.copy(favoriteSongIds = ids.toSet()) }
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

    fun loadLibrary(hasPermission: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasPermission = hasPermission) }
            if (hasPermission) {
                try {
                    val songs = repository.getAllSongs()
                    val albums = repository.getAlbums()
                    val artists = repository.getArtists()
                    _uiState.update { it.copy(
                        allSongs = songs, albums = albums, artists = artists, isLoading = false
                    )}
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update { it.copy(searchResults = emptyList()) }
                return@launch
            }
            val results = repository.searchSongs(query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun playSong(song: Song) {
        val queue = _uiState.value.allSongs.ifEmpty { listOf(song) }
        val idx = queue.indexOf(song).coerceAtLeast(0)
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

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            if (songId in _uiState.value.favoriteSongIds) {
                repository.removeFavorite(songId)
            } else {
                repository.addFavorite(songId)
            }
        }
    }

    fun getFavoriteSongs(): List<Song> {
        val favIds = _uiState.value.favoriteSongIds
        return _uiState.value.allSongs.filter { it.id in favIds }
    }

    fun getRecentlyAddedSongs(limit: Int = 10): List<Song> =
        _uiState.value.allSongs.sortedByDescending { it.dateAdded }.take(limit)
}
