package com.muhil.music.ui.screens.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muhil.music.ui.components.*
import com.muhil.music.ui.navigation.Screen
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

@Composable
fun AllSongsScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playback by viewModel.playbackState.collectAsState()

    Scaffold(
        containerColor = BgBlack,
        topBar = {
            MuhilTopBar(
                title = "All Songs (${uiState.allSongs.size})",
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = {
                        if (uiState.allSongs.isNotEmpty()) {
                            viewModel.playSongs(uiState.allSongs.shuffled())
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle All", tint = GoldPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.allSongs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    isFavorite = song.id in uiState.favoriteSongIds,
                    isPlaying = playback.currentSong?.id == song.id && playback.isPlaying,
                    onClick = {
                        viewModel.playSong(song)
                        navController.navigate(Screen.NowPlaying.route)
                    },
                    onFavoriteClick = { viewModel.toggleFavorite(song) }
                )
                GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun AlbumsScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgBlack,
        topBar = { MuhilTopBar(title = "Albums", onBack = { navController.popBackStack() }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.albums, key = { it.id }) { album ->
                AlbumCard(
                    title = album.title,
                    artist = album.artist,
                    artUri = album.artUri,
                    songCount = album.songCount,
                    onClick = { /* TODO: album detail */ }
                )
            }
        }
    }
}

@Composable
fun AlbumCard(title: String, artist: String, artUri: String?, songCount: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            AlbumArtImage(
                uri = artUri,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                cornerRadius = 0.dp
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary
                ), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$artist • $songCount songs", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ArtistsScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgBlack,
        topBar = { MuhilTopBar(title = "Artists", onBack = { navController.popBackStack() }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.artists, key = { it.id }) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: artist detail */ }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImage(uri = artist.artUri, modifier = Modifier.size(56.dp), cornerRadius = 28.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold, color = TextPrimary
                        ))
                        Text("${artist.songCount} songs • ${artist.albumCount} albums",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary)
                }
                GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val favSongs = uiState.favoriteSongs

    Scaffold(
        containerColor = BgBlack,
        topBar = { MuhilTopBar(title = "Favorites (${favSongs.size})", onBack = { navController.popBackStack() }) }
    ) { padding ->
        if (favSongs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Favorites Yet", style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    Text("Tap the heart icon on a song to add it here", style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary),
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(favSongs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isFavorite = true,
                        isPlaying = playback.currentSong?.id == song.id && playback.isPlaying,
                        onClick = { viewModel.playSong(song); navController.navigate(Screen.NowPlaying.route) },
                        onFavoriteClick = { viewModel.toggleFavorite(song) }
                    )
                    GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playback by viewModel.playbackState.collectAsState()

    Scaffold(
        containerColor = BgBlack,
        topBar = { MuhilTopBar(title = "Recently Played", onBack = { navController.popBackStack() }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(uiState.recentlyPlayed, key = { it.songId }) { recent ->
                val song = uiState.allSongs.find { it.id == recent.songId } ?: recent.toSong()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.playSong(song)
                            navController.navigate(Screen.NowPlaying.route)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImage(uri = recent.albumArtUri, modifier = Modifier.size(52.dp), cornerRadius = 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(recent.title, style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(recent.artist, style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary), maxLines = 1)
                    }
                    Icon(Icons.Rounded.History, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
                GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
