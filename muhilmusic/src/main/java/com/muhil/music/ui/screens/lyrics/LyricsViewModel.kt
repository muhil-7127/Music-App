package com.muhil.music.ui.screens.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muhil.music.data.repository.MusicRepository
import com.muhil.music.domain.model.LyricLine
import com.muhil.music.domain.model.LyricsState
import com.muhil.music.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadLyrics(song: Song) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            _currentLineIndex.value = -1
            try {
                val state = repository.getLyrics(song)
                _lyricsState.value = state
                syncedLines = if (state is LyricsState.Synced) state.lines else emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Error loading lyrics for ${song.title}")
                _lyricsState.value = LyricsState.Unavailable
                syncedLines = emptyList()
            }
        }
    }

    fun updatePosition(positionMs: Long) {
        val lines = syncedLines
        if (lines.isEmpty()) return
        val index = lines.indexOfLast { it.timestampMs <= positionMs }
        if (index != _currentLineIndex.value) {
            _currentLineIndex.value = index
        }
    }
}
