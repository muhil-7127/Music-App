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
import com.muhil.music.domain.model.Song
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

    // Permission launcher for secondary local MP3 feature
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.loadLocalLibrary(granted) }

    val genres = remember {
        listOf("All", "Pop", "Rock", "Hip-Hop", "R&B", "Electronic", "Tamil", "Bollywood", "Classical", "Indie")
    }

    Scaffold(
        containerColor = BgBlack,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 1. Header with greeting and Muhil Logo
                item {
                    HomeHeader(
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                // 2. Current-song mini controller near top of screen (as requested)
                if (playbackState.currentSong != null) {
                    item {
                        MiniNowPlayingBar(
                            song = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            onPlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::next,
                            onClick = { navController.navigate(Screen.NowPlaying.route) },
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                }

                // 3. Quick Navigation Chips (Library, Playlists, Favorites, Local)
                item {
                    HomeQuickNavRow(
                        navController = navController,
                        onLocalClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                            navController.navigate(Screen.AllSongs.route)
                        }
                    )
                }

                // 4. Genre Filter Discovery Chips
                item {
                    Text(
                        text = "Explore Genres",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(genres) { genre ->
                            val isSelected = uiState.selectedGenre.equals(genre, ignoreCase = true)
                            Surface(
                                onClick = { viewModel.selectGenre(genre) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) GoldPrimary else BgCard,
                                border = BorderStroke(1.dp, if (isSelected) GoldLight else GoldPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BgBlack else TextPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 5. Genre-Specific Songs (when a specific genre is selected)
                if (uiState.selectedGenre != "All" && uiState.genreSongs.isNotEmpty()) {
                    item {
                        SectionHeader("${uiState.selectedGenre} Hits") {}
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.genreSongs, key = { it.id }) { song ->
                                FeaturedSongCard(
                                    song = song,
                                    isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                                    onClick = {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 6. Featured Web Songs (Primary Online Discovery)
                item {
                    SectionHeader("Featured Web Tracks") {
                        navController.navigate(Screen.Search.route)
                    }
                }

                if (uiState.isLoading && uiState.featuredSongs.isEmpty()) {
                    item { HomeLoadingSkeleton() }
                } else if (uiState.error != null && uiState.featuredSongs.isEmpty()) {
                    item {
                        HomeErrorBanner(
                            message = uiState.error ?: "Unable to load online tracks. Check network.",
                            onRetry = { viewModel.loadHomeContent(forceRefresh = true) }
                        )
                    }
                } else if (uiState.featuredSongs.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.featuredSongs, key = { it.id }) { song ->
                                FeaturedSongCard(
                                    song = song,
                                    isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                                    onClick = {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 6b. Intelligent Similar-Song Recommendations ("Similar Vibes" / "Made For You")
                if (uiState.recommendations.isNotEmpty()) {
                    val seedTitle = uiState.recommendationSeed?.title
                    val recHeading = if (!seedTitle.isNullOrBlank()) "Similar Vibes • Because of $seedTitle" else "Made For You"
                    item {
                        SectionHeader(recHeading) {}
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.recommendations, key = { it.id }) { recSong ->
                                FeaturedSongCard(
                                    song = recSong,
                                    isPlaying = playbackState.currentSong?.id == recSong.id && playbackState.isPlaying,
                                    onClick = {
                                        viewModel.playSong(recSong)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 7. Popular Artists
                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        SectionHeader("Popular Artists") {}
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.topArtists, key = { it.id }) { artist ->
                                ArtistCircleCard(
                                    artist = artist,
                                    onClick = {
                                        viewModel.search(artist.name)
                                        navController.navigate(Screen.Search.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 8. Trending Albums
                if (uiState.trendingAlbums.isNotEmpty()) {
                    item {
                        SectionHeader("Trending Albums") {}
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.trendingAlbums, key = { it.id }) { album ->
                                AlbumSquareCard(
                                    album = album,
                                    onClick = {
                                        viewModel.search(album.title)
                                        navController.navigate(Screen.Search.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 9. Recently Played
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader("Recently Played") { navController.navigate(Screen.RecentPlay.route) }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentlyPlayed.take(8), key = { it.songId }) { recent ->
                                MiniSongCard(
                                    title = recent.title,
                                    artist = recent.artist,
                                    artUri = recent.albumArtUri,
                                    onClick = {
                                        viewModel.playSong(recent.toSong())
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 10. Favorites
                if (uiState.favoriteSongs.isNotEmpty()) {
                    item {
                        SectionHeader("Favorites") { navController.navigate(Screen.Favorites.route) }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.favoriteSongs.take(8), key = { it.id }) { song ->
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

                // 11. Local Device Audio Banner (Secondary Feature)
                item {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                            navController.navigate(Screen.AllSongs.route)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = BgCard,
                        border = BorderStroke(1.dp, SilverDark.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Device Audio Files",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    "Tap to browse and play MP3 files stored on this phone",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                                )
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = SilverDark
                            )
                        }
                    }
                }
            }

            // 12. Floating Search Bar near bottom of screen (as requested)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                FloatingSearchBar(onClick = { navController.navigate(Screen.Search.route) })
            }
        }
    }
}

@Composable
fun HomeHeader(onSettingsClick: () -> Unit) {
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
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = "android.resource://com.muhil.music/drawable/muhil_logo",
                contentDescription = "Muhil Logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.dp, GoldPrimary, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Muhil Music",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                    )
                )
                Text(greeting, style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = SilverLight)
            }
        }
    }
}

@Composable
fun HomeQuickNavRow(
    navController: NavController,
    onLocalClick: () -> Unit
) {
    val items = listOf(
        Triple(Icons.Rounded.Favorite,   "Favorites",  { navController.navigate(Screen.Favorites.route) }),
        Triple(Icons.Rounded.QueueMusic, "Playlists",  { navController.navigate(Screen.Playlists.route) }),
        Triple(Icons.Rounded.Album,      "Albums",     { navController.navigate(Screen.Albums.route) }),
        Triple(Icons.Rounded.Person,     "Artists",    { navController.navigate(Screen.Artists.route) }),
        Triple(Icons.Rounded.Folder,     "Local MP3s", onLocalClick)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { (icon, label, onClick) ->
            QuickNavChip(icon = icon, label = label, onClick = onClick)
        }
    }
}

@Composable
fun QuickNavChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = BgCard,
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
    }
}

@Composable
fun MiniSongCard(title: String, artist: String, artUri: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumArtImage(
            uri = artUri,
            modifier = Modifier.size(105.dp),
            cornerRadius = 12.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            artist,
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FloatingSearchBar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = BgElevated,
        shadowElevation = 14.dp,
        border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.6f)),
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Search web songs, artists, albums...",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
            )
        }
    }
}

@Composable
fun HomeLoadingSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            Column(modifier = Modifier.width(160.dp)) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GoldPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BgCard)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BgCard)
                )
            }
        }
    }
}

@Composable
fun HomeErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Connection Issue",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                Text(message, style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
            }
            TextButton(onClick = onRetry) {
                Text("Retry", style = MaterialTheme.typography.labelLarge.copy(color = GoldPrimary, fontWeight = FontWeight.Bold))
            }
        }
    }
}
