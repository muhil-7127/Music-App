package com.muhil.music.ui.screens.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muhil.music.data.repository.MusicRepository
import com.muhil.music.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    private var syncedLines: List<LyricLine> = emptyList()

    fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            // Check cache first
            val cached = repository.getCachedLyrics(song.id)
            if (cached != null) {
                if (cached.isTimestamped) {
                    val lines = parseLrc(cached.lyricsText)
                    syncedLines = lines
                    _lyricsState.value = LyricsState.Synced(lines)
                } else {
                    _lyricsState.value = LyricsState.Plain(cached.lyricsText)
                }
                return@launch
            }

            // Try to find .lrc file alongside the audio file
            val lrcPath = song.uri.replace(Regex("\\.[^.]+$"), ".lrc")
            try {
                // Try local .lrc discovery via URI path manipulation
                val lrcFile = java.io.File(lrcPath.removePrefix("content://").let { song.uri.substringAfterLast("/").replace(Regex("\\.[^.]+$"), ".lrc") })
                if (lrcFile.exists()) {
                    val text = lrcFile.readText()
                    val lines = parseLrc(text)
                    if (lines.isNotEmpty()) {
                        syncedLines = lines
                        _lyricsState.value = LyricsState.Synced(lines)
                        repository.cacheLyrics(song.id, text, true)
                    } else {
                        _lyricsState.value = LyricsState.Plain(text)
                        repository.cacheLyrics(song.id, text, false)
                    }
                } else {
                    _lyricsState.value = LyricsState.Unavailable
                }
            } catch (e: Exception) {
                _lyricsState.value = LyricsState.Unavailable
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

    /** Parse LRC format: [mm:ss.xx]Line text */
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
}
