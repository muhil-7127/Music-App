package com.muhil.music.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.muhil.music.ui.components.*
import com.muhil.music.ui.navigation.Screen
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val song = playback.currentSong

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

            Spacer(Modifier.height(32.dp))

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
                IconButton(onClick = { song?.let { viewModel.toggleFavorite(it.id) } }) {
                    Icon(
                        imageVector = if (song != null && song.id in uiState.favoriteSongIds)
                            Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

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
                // Play/Pause
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
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = BgBlack,
                        modifier = Modifier.size(38.dp)
                    )
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

            // Queue button
            TextButton(onClick = { /* TODO: Show queue */ }) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Queue", style = MaterialTheme.typography.labelLarge.copy(color = GoldPrimary))
            }
        }
    }
}
