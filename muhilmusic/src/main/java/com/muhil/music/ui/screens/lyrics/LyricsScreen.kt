package com.muhil.music.ui.screens.lyrics

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muhil.music.domain.model.LyricsState
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

@Composable
fun LyricsScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val playback by viewModel.playbackState.collectAsState()
    val song = playback.currentSong
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    val lyricsState by lyricsViewModel.lyricsState.collectAsState()
    val currentLine by lyricsViewModel.currentLineIndex.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(song) {
        song?.let { lyricsViewModel.loadLyrics(it) }
    }

    LaunchedEffect(currentLine) {
        if (currentLine >= 0) {
            listState.animateScrollToItem(currentLine.coerceAtLeast(0), scrollOffset = -200)
        }
    }

    LaunchedEffect(playback.positionMs) {
        lyricsViewModel.updatePosition(playback.positionMs)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Back",
                        tint = SilverMid, modifier = Modifier.size(32.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(song?.title ?: "Lyrics", style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = TextPrimary
                    ))
                    Text(song?.artist ?: "", style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
                }
                Text("Lyrics", style = MaterialTheme.typography.labelLarge.copy(color = GoldPrimary))
                Spacer(Modifier.width(16.dp))
            }

            when (val state = lyricsState) {
                is LyricsState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GoldPrimary)
                            Spacer(Modifier.height(16.dp))
                            Text("Loading lyrics...", color = SilverMid)
                        }
                    }
                }
                is LyricsState.Unavailable -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.MusicOff, contentDescription = null,
                                tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No lyrics available", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                            Text("Lyrics could not be found for this song online", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                                modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
                is LyricsState.Plain -> {
                    Text(
                        text = state.text,
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary, lineHeight = 28.sp),
                        modifier = Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    )
                }
                is LyricsState.Synced -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(state.lines) { index, line ->
                            val isActive = index == currentLine
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) GoldPrimary else TextTertiary.copy(alpha = if (index < currentLine) 0.5f else 0.85f),
                                    fontSize = if (isActive) 20.sp else 16.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = if (isActive) 4.dp else 0.dp)
                                    .animateItem()
                            )
                        }
                    }
                }
                is LyricsState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = ErrorRed)
                    }
                }
            }
        }
    }
}
