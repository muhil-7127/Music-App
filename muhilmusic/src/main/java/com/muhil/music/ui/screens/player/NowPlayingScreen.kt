package com.muhil.music.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muhil.music.domain.model.RepeatMode
import com.muhil.music.domain.model.Song
import com.muhil.music.ui.components.*
import com.muhil.music.ui.navigation.Screen
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val sleepTimerState by viewModel.sleepTimerState.collectAsState()
    val song = playback.currentSong
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack, BgBlack)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            // Top bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Back", tint = SilverMid, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("Now Playing", style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary
                ))
                Spacer(Modifier.weight(1f))

                // Sleep Timer icon / active countdown pill
                if (sleepTimerState.isActive) {
                    Surface(
                        onClick = { showSleepTimerDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = GoldPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, GoldPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = sleepTimerState.formattedRemaining,
                                style = MaterialTheme.typography.labelSmall.copy(color = GoldPrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = "Sleep Timer", tint = SilverMid)
                    }
                }

                // Equalizer button
                IconButton(onClick = { navController.navigate(Screen.Equalizer.route) }) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = "Equalizer", tint = GoldPrimary)
                }

                // Lyrics button
                IconButton(onClick = { navController.navigate(Screen.Lyrics.route) }) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics", tint = GoldPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Album Art with animated glow
            val infiniteTransition = rememberInfiniteTransition(label = "artGlow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.02f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
                label = "glowScale"
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(if (playback.isPlaying) glowScale else 1f)
                    .shadow(elevation = if (playback.isPlaying) 24.dp else 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = GoldPrimary.copy(alpha = 0.3f),
                        spotColor = GoldPrimary.copy(alpha = 0.3f))
            ) {
                AlbumArtImage(
                    uri = song?.albumArtUri,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 24.dp
                )
            }

            Spacer(Modifier.height(28.dp))

            // Playback Error Banner if present
            if (playback.errorMessage != null) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = playback.errorMessage ?: "Stream issue",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFCA5A5)),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Song info + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "No Song Playing",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold, color = TextPrimary
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song?.artist ?: "—",
                            style = MaterialTheme.typography.bodyLarge.copy(color = SilverMid),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (song?.isPreview == true) {
                            Spacer(Modifier.width(8.dp))
                            WebPreviewBadge()
                        } else if (song?.isOnline == true) {
                            Spacer(Modifier.width(8.dp))
                            FullTrackBadge()
                        }
                    }
                }
                IconButton(onClick = { song?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (song != null && song.id in uiState.favoriteSongIds)
                            Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = playback.progress,
                    onValueChange = { viewModel.seekTo((it * playback.durationMs).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = BgElevated
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(playback.positionMs), style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary))
                    Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = viewModel::toggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playback.shuffleEnabled) GoldPrimary else SilverDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Previous
                IconButton(onClick = viewModel::previous, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous",
                        tint = SilverLight, modifier = Modifier.size(36.dp))
                }
                // Play/Pause with Buffering indicator
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(GoldPrimary, GoldDark)),
                            shape = CircleShape
                        )
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (playback.isBuffering) {
                        CircularProgressIndicator(
                            color = BgBlack,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = BgBlack,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                // Next
                IconButton(onClick = viewModel::next, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next",
                        tint = SilverLight, modifier = Modifier.size(36.dp))
                }
                // Repeat
                IconButton(onClick = viewModel::toggleRepeat) {
                    Icon(
                        imageVector = when (playback.repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = when (playback.repeatMode) {
                            RepeatMode.OFF -> SilverDark
                            else -> GoldPrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Queue and Similar Vibes button
            TextButton(onClick = { showQueueSheet = true }) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Queue & Similar Vibes", style = MaterialTheme.typography.labelLarge.copy(color = GoldPrimary))
            }
        }

        // Modal Bottom Sheet for Queue & Recommendations
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = BgElevated,
                dragHandle = { BottomSheetDefaults.DragHandle(color = GoldPrimary.copy(alpha = 0.5f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Playing Queue (${playback.queue.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(playback.queue, key = { it.id }) { qSong: Song ->
                            val isCurrent = qSong.id == playback.currentSong?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) GoldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { viewModel.playSong(qSong) }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtImage(uri = qSong.albumArtUri, modifier = Modifier.size(40.dp), cornerRadius = 6.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        qSong.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isCurrent) GoldLight else TextPrimary,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(qSong.artist, style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary), maxLines = 1)
                                }
                                if (isCurrent) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    if (uiState.recommendations.isNotEmpty()) {
                        Text(
                            "Similar Vibes (Recommended Next)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(uiState.recommendations, key = { it.id }) { recSong: Song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playSong(recSong) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlbumArtImage(uri = recSong.albumArtUri, modifier = Modifier.size(40.dp), cornerRadius = 6.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recSong.title, style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(recSong.artist, style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary), maxLines = 1)
                                    }
                                    IconButton(onClick = { viewModel.addToQueue(recSong) }) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Add to Queue", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                containerColor = BgCard,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bedtime, null, tint = GoldPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                text = {
                    Column {
                        if (sleepTimerState.isActive) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Timer Active", style = MaterialTheme.typography.labelSmall.copy(color = GoldPrimary))
                                        Text(
                                            "${sleepTimerState.formattedRemaining} remaining",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                    }
                                    TextButton(onClick = {
                                        viewModel.cancelSleepTimer()
                                        showSleepTimerDialog = false
                                    }) {
                                        Text("Turn Off", color = ErrorRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Text("Stop playback after:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Spacer(Modifier.height(12.dp))

                        val presetsRow1 = listOf(5, 10, 15)
                        val presetsRow2 = listOf(30, 45, 60)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetsRow1.forEach { minutes ->
                                val isSelected = sleepTimerState.isActive && sleepTimerState.totalMinutes == minutes
                                Surface(
                                    onClick = {
                                        viewModel.startSleepTimer(minutes)
                                        showSleepTimerDialog = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldPrimary else BgElevated,
                                    border = BorderStroke(1.dp, if (isSelected) GoldLight else SilverDark.copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$minutes min",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isSelected) BgBlack else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetsRow2.forEach { minutes ->
                                val isSelected = sleepTimerState.isActive && sleepTimerState.totalMinutes == minutes
                                Surface(
                                    onClick = {
                                        viewModel.startSleepTimer(minutes)
                                        showSleepTimerDialog = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldPrimary else BgElevated,
                                    border = BorderStroke(1.dp, if (isSelected) GoldLight else SilverDark.copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$minutes min",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isSelected) BgBlack else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Close", color = GoldPrimary)
                    }
                }
            )
        }
    }
}
