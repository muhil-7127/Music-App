package com.muhil.music.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.muhil.music.domain.model.PlaybackState
import com.muhil.music.domain.model.RepeatMode
import com.muhil.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MuhilPlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controller: MediaController? = null

    private var pendingAction: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) { syncState() }
        override fun onIsPlayingChanged(isPlaying: Boolean) { syncState() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            controller?.let { c ->
                val idx = c.currentMediaItemIndex
                val queue = _playbackState.value.queue
                val newSong = queue.getOrNull(idx)
                if (newSong != null) {
                    _playbackState.value = _playbackState.value.copy(
                        queueIndex = idx,
                        currentSong = newSong,
                        errorMessage = null
                    )
                }
            }
            syncState()
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { syncState() }
        override fun onRepeatModeChanged(repeatMode: Int) { syncState() }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            timber.log.Timber.e(error, "ExoPlayer playback error: ${error.message}")
            _playbackState.value = _playbackState.value.copy(
                isBuffering = false,
                errorMessage = "Playback failed: ${error.localizedMessage ?: "Network or stream issue"}"
            )
            syncState()
        }
    }

    init {
        connectToService()
        // Poll position every second
        scope.launch {
            while (true) {
                delay(1000)
                syncState()
            }
        }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MuhilMusicService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken)
            .buildAsync()
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(playerListener)
                syncState()
                pendingAction?.invoke()
                pendingAction = null
            } catch (e: Exception) {
                // Service not yet ready
            }
        }, MoreExecutors.directExecutor())
    }

    private fun syncState() {
        val c = controller ?: return
        val current = _playbackState.value
        val duration = c.duration.coerceAtLeast(0L)
        val position = c.currentPosition.coerceAtLeast(0L)
        val repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
        val isBuffering = c.playbackState == Player.STATE_BUFFERING
        _playbackState.value = current.copy(
            isPlaying      = c.isPlaying,
            isBuffering    = isBuffering,
            positionMs     = position,
            durationMs     = duration,
            progress       = if (duration > 0) position.toFloat() / duration else 0f,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode     = repeatMode
        )
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val c = controller
        if (c == null) {
            pendingAction = { playSongs(songs, startIndex) }
            connectToService()
            return
        }
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
        _playbackState.value = _playbackState.value.copy(
            queue        = songs,
            queueIndex   = startIndex,
            currentSong  = songs.getOrNull(startIndex),
            errorMessage = null,
            isBuffering  = true
        )
    }

    fun addToQueue(song: Song) {
        val c = controller
        val currentQueue = _playbackState.value.queue
        if (currentQueue.any { it.id == song.id }) return // Already in queue

        val newQueue = currentQueue + song
        _playbackState.value = _playbackState.value.copy(queue = newQueue)
        c?.addMediaItem(song.toMediaItem())
    }

    fun addToQueue(songs: List<Song>) {
        val c = controller
        val currentQueue = _playbackState.value.queue
        val existingIds = currentQueue.map { it.id }.toSet()
        val toAdd = songs.filter { it.id !in existingIds }
        if (toAdd.isEmpty()) return

        val newQueue = currentQueue + toAdd
        _playbackState.value = _playbackState.value.copy(queue = newQueue)
        toAdd.forEach { c?.addMediaItem(it.toMediaItem()) }
    }

    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
}
