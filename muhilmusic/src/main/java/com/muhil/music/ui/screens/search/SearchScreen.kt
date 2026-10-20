package com.muhil.music.ui.screens.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muhil.music.ui.components.*
import com.muhil.music.ui.navigation.Screen
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

@Composable
fun SearchScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) { viewModel.search(query) }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MuhilTopBar(title = "Search", onBack = { navController.popBackStack() })

            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Songs, artists, albums...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = GoldPrimary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, null, tint = SilverDark)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = SilverDark.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GoldPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true
            )

            if (query.isBlank()) {
                // Trending / suggestions
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Search, null, tint = GoldPrimary.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Search your music library", style = MaterialTheme.typography.titleMedium.copy(color = TextTertiary))
                }
            } else {
                LazyColumn {
                    if (uiState.searchResults.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.SearchOff, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("No results for \"$query\"", style = MaterialTheme.typography.titleMedium.copy(color = TextTertiary))
                                }
                            }
                        }
                    } else {
                        item {
                            Text("${uiState.searchResults.size} results", 
                                style = MaterialTheme.typography.labelMedium.copy(color = SilverDark),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                        items(uiState.searchResults) { song ->
                            SongListItem(
                                song = song,
                                isFavorite = song.id in uiState.favoriteSongIds,
                                isPlaying = playback.currentSong?.id == song.id && playback.isPlaying,
                                onClick = {
                                    viewModel.playSong(song)
                                    navController.navigate(Screen.NowPlaying.route)
                                },
                                onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                            )
                            GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
