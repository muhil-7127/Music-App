package com.muhil.music.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import coil3.compose.AsyncImage
import com.muhil.music.ui.components.*
import com.muhil.music.ui.navigation.Screen
import com.muhil.music.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.loadLibrary(granted) }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        permissionLauncher.launch(permission)
    }

    Scaffold(
        containerColor = BgBlack,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                // Header with greeting and logo
                item {
                    HomeHeader(
                        onSearchClick = { navController.navigate(Screen.Search.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                // Mini Now Playing
                if (playbackState.currentSong != null) {
                    item {
                        MiniNowPlayingBar(
                            song = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            onPlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::next,
                            onClick = { navController.navigate(Screen.NowPlaying.route) }
                        )
                    }
                }

                // Quick Nav Chips
                item {
                    QuickNavRow(navController = navController)
                }

                // Recently Played
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader("Recently Played") { navController.navigate(Screen.RecentPlay.route) }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentlyPlayed.take(8)) { recent ->
                                val song = uiState.allSongs.find { it.id == recent.songId }
                                MiniSongCard(
                                    title = recent.title,
                                    artist = recent.artist,
                                    artUri = recent.albumArtUri,
                                    onClick = {
                                        song?.let { viewModel.playSong(it) }
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // Favorites
                val favSongs = viewModel.getFavoriteSongs()
                if (favSongs.isNotEmpty()) {
                    item {
                        SectionHeader("Favorites") { navController.navigate(Screen.Favorites.route) }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favSongs.take(8)) { song ->
                                MiniSongCard(
                                    title = song.title,
                                    artist = song.artist,
                                    artUri = song.albumArtUri,
                                    onClick = {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // Recently Added
                val recentlyAdded = viewModel.getRecentlyAddedSongs()
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        SectionHeader("Recently Added") { navController.navigate(Screen.AllSongs.route) }
                    }
                    items(recentlyAdded.take(5)) { song ->
                        SongListItem(
                            song = song,
                            isFavorite = song.id in uiState.favoriteSongIds,
                            isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playSong(song)
                                navController.navigate(Screen.NowPlaying.route)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                        )
                        GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                // Empty state
                if (!uiState.isLoading && uiState.allSongs.isEmpty() && uiState.hasPermission) {
                    item {
                        EmptyLibraryState(onScan = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                        })
                    }
                }

                // Permission denied
                if (!uiState.hasPermission && !uiState.isLoading) {
                    item { PermissionDeniedState(onGrant = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                        permissionLauncher.launch(permission)
                    }) }
                }
            }

            // Bottom Search Bar
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
                FloatingSearchBar(onClick = { navController.navigate(Screen.Search.route) })
            }
        }
    }
}

@Composable
fun HomeHeader(onSearchClick: () -> Unit, onSettingsClick: () -> Unit) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "android.resource://com.muhil.music/drawable/muhil_logo",
                    contentDescription = "Muhil Logo",
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Muhil Music", style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                    ))
                    Text(greeting, style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = SilverDark)
                }
            }
        }
    }
}

@Composable
fun QuickNavRow(navController: NavController) {
    val items = listOf(
        Triple(Icons.Rounded.LibraryMusic, "Songs",    Screen.AllSongs.route),
        Triple(Icons.Rounded.Album,         "Albums",   Screen.Albums.route),
        Triple(Icons.Rounded.Person,        "Artists",  Screen.Artists.route),
        Triple(Icons.Rounded.QueueMusic,    "Playlists",Screen.Playlists.route),
        Triple(Icons.Rounded.Favorite,      "Favorites",Screen.Favorites.route)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { (icon, label, route) ->
            QuickNavChip(icon = icon, label = label, onClick = { navController.navigate(route) })
        }
    }
}

@Composable
fun QuickNavChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = BgCard,
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary))
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold, color = TextPrimary
        ))
        TextButton(onClick = onSeeAll) {
            Text("See All", style = MaterialTheme.typography.labelMedium.copy(color = GoldPrimary))
        }
    }
}

@Composable
fun MiniSongCard(title: String, artist: String, artUri: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumArtImage(
            uri = artUri,
            modifier = Modifier.size(110.dp),
            cornerRadius = 12.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold, color = TextPrimary
        ), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist, style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FloatingSearchBar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = BgCard,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Search songs, artists, albums...", style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary))
        }
    }
}

@Composable
fun EmptyLibraryState(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.MusicOff, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("No Music Found", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        Text("Add MP3 files to your device to start listening", style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        GoldGradientButton(text = "Scan Library", onClick = onScan, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PermissionDeniedState(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.FolderOff, contentDescription = null, tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("Permission Required", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
        Text("Muhil Music needs access to your audio files to play music", style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        GoldGradientButton(text = "Grant Permission", onClick = onGrant, modifier = Modifier.fillMaxWidth())
    }
}
