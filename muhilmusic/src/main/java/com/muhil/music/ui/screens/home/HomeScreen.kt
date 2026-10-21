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
import com.muhil.music.domain.recommendation.Mood
import com.muhil.music.domain.search.MovieSoundtrack
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

    // Permission launcher for local MP3 feature
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.loadLocalLibrary(granted) }

    val genres = remember {
        listOf("All", "Tamil", "Pop", "Rock", "Hip-Hop", "R&B", "Melody", "Electronic", "Classical", "Indie")
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
                // 1. Header with greeting, user profile picture, and language selector
                item {
                    HomeHeader(
                        userName = uiState.userName,
                        profilePictureUri = uiState.profilePictureUri,
                        currentLanguage = uiState.preferredLanguage,
                        onLanguageSelected = { lang -> viewModel.setPreferredLanguage(lang) },
                        onProfileClick = { navController.navigate(Screen.Settings.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                // 2. Prominent Search Bar
                item {
                    ProminentSearchBar(
                        onClick = { navController.navigate(Screen.Search.route) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // 3. Mini-Player bar near top if a song is currently active
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

                // 4. Quick Navigation Chips (Favorites, Playlists, Albums, Artists, Local MP3s)
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

                // 4b. Mood-Based Discovery Selector Chips
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HOW ARE YOU FEELING?",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            if (uiState.selectedMood != null) {
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                                    modifier = Modifier.clickable { viewModel.selectMood(uiState.selectedMood) }
                                )
                            }
                        }
                        HomeMoodChipsRow(
                            selectedMood = uiState.selectedMood,
                            onMoodSelected = { viewModel.selectMood(it) }
                        )
                    }
                }

                // 5. Genre Filter Discovery Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                }

                // 6. Genre-Specific Songs (when a specific genre is selected)
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
                                    isFavorite = song.id in uiState.favoriteSongIds,
                                    onFavoriteClick = { viewModel.toggleFavorite(song) },
                                    onClick = {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 7. TAMIL-FIRST DISCOVERY SECTIONS (Displayed prominently for Tamil listeners)
                if (uiState.preferredLanguage == "Tamil" || uiState.preferredLanguage == "All") {
                    // 7a. Tamil Hits
                    if (uiState.tamilHits.isNotEmpty()) {
                        item {
                            SectionHeader("🔥 Tamil Hits 2024") {
                                viewModel.search("Tamil Hits")
                                navController.navigate(Screen.Search.route)
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.tamilHits, key = { it.id }) { song ->
                                    FeaturedSongCard(
                                        song = song,
                                        isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                                        isFavorite = song.id in uiState.favoriteSongIds,
                                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                                        onClick = {
                                            viewModel.playSong(song)
                                            navController.navigate(Screen.NowPlaying.route)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 7b. Tamil Movie Soundtracks
                    if (uiState.tamilSoundtracks.isNotEmpty()) {
                        item {
                            SectionHeader("🎬 Tamil Movie Soundtracks") {
                                viewModel.search("Tamil Soundtracks")
                                navController.navigate(Screen.Search.route)
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.tamilSoundtracks, key = { it.movieName }) { soundtrack ->
                                    SoundtrackMiniCard(
                                        soundtrack = soundtrack,
                                        onClick = {
                                            viewModel.playSongs(soundtrack.tracks, 0)
                                            navController.navigate(Screen.NowPlaying.route)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 7c. Tamil Melodies & Love Songs
                    if (uiState.tamilMelodies.isNotEmpty()) {
                        item {
                            SectionHeader("💖 Tamil Melodies & Love Songs") {
                                viewModel.search("Tamil Melody")
                                navController.navigate(Screen.Search.route)
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.tamilMelodies, key = { it.id }) { song ->
                                    FeaturedSongCard(
                                        song = song,
                                        isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                                        isFavorite = song.id in uiState.favoriteSongIds,
                                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                                        onClick = {
                                            viewModel.playSong(song)
                                            navController.navigate(Screen.NowPlaying.route)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 7d. Top Tamil Composers
                    if (uiState.tamilArtists.isNotEmpty()) {
                        item {
                            SectionHeader("🎵 Top Tamil Composers & Singers") {}
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.tamilArtists, key = { it.id }) { artist ->
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

                    // 7e. Evergreen Tamil Classics
                    if (uiState.tamilClassics.isNotEmpty()) {
                        item {
                            SectionHeader("✨ Evergreen Tamil Classics") {
                                viewModel.search("Tamil Classics")
                                navController.navigate(Screen.Search.route)
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.tamilClassics, key = { it.id }) { song ->
                                    FeaturedSongCard(
                                        song = song,
                                        isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                                        isFavorite = song.id in uiState.favoriteSongIds,
                                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                                        onClick = {
                                            viewModel.playSong(song)
                                            navController.navigate(Screen.NowPlaying.route)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 8. ADAPTIVE PERSONALIZED RECOMMENDATIONS ("Similar Vibes" / "Made For You")
                if (uiState.recommendations.isNotEmpty()) {
                    val seedTitle = uiState.recommendationSeed?.title
                    val recHeading = if (uiState.selectedMood != null) {
                        "${uiState.selectedMood?.emoji} ${uiState.selectedMood?.displayName} Vibes • Made For You"
                    } else if (!seedTitle.isNullOrBlank()) {
                        "Similar Vibes • Because of $seedTitle"
                    } else {
                        "Made For You • Recommendations"
                    }
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
                                    isFavorite = recSong.id in uiState.favoriteSongIds,
                                    onFavoriteClick = { viewModel.toggleFavorite(recSong) },
                                    onClick = {
                                        viewModel.playSong(recSong)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 9. Featured Global Web Tracks
                item {
                    SectionHeader("Featured Global Tracks") {
                        navController.navigate(Screen.Search.route)
                    }
                }

                if (uiState.isLoading && uiState.featuredSongs.isEmpty() && uiState.tamilHits.isEmpty()) {
                    item { HomeLoadingSkeleton() }
                } else if (uiState.error != null && uiState.featuredSongs.isEmpty() && uiState.tamilHits.isEmpty()) {
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
                                    isFavorite = song.id in uiState.favoriteSongIds,
                                    onFavoriteClick = { viewModel.toggleFavorite(song) },
                                    onClick = {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                            }
                        }
                    }
                }

                // 10. Popular Artists
                if (uiState.topArtists.isNotEmpty()) {
                    item {
                        SectionHeader("Global Artists") {}
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

                // 11. Trending Albums
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

                // 12. Recently Played
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

                // 13. Favorites
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

                // 14. Local Device Audio Banner (Secondary Feature)
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

            // Floating Search Button near bottom
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
fun ProminentSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Search song, Tamil movie (Leo, Jailer), artist...",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeHeader(
    userName: String = "Muhil",
    profilePictureUri: String? = null,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("Tamil", "English", "Hindi", "All")

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
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, GoldPrimary, CircleShape)
                    .clickable { onProfileClick() }
            ) {
                AsyncImage(
                    model = profilePictureUri ?: "android.resource://com.muhil.music/drawable/muhil_logo",
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Muhil Music",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                    )
                )
                Text("$greeting, $userName", style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
            }

            // Language Selector Pill with Dropdown
            Box {
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = BgCard,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌐 $currentLanguage ▾",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(BgCard)
                ) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (lang == "Tamil") "Tamil (தமிழ்)" else lang,
                                    color = if (lang == currentLanguage) GoldPrimary else TextPrimary,
                                    fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onLanguageSelected(lang)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = SilverLight)
            }
        }
    }
}

@Composable
fun SoundtrackMiniCard(
    soundtrack: MovieSoundtrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgBlack),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    uri = soundtrack.albumArtUri,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 12.dp
                )
                // Overlay play icon
                Surface(
                    shape = CircleShape,
                    color = GoldPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play Album",
                        tint = BgBlack,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = soundtrack.movieName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${soundtrack.tracks.size} Songs • ${soundtrack.composer}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeMoodChipsRow(
    selectedMood: Mood?,
    onMoodSelected: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(Mood.entries.toTypedArray()) { mood ->
            val isSelected = selectedMood == mood
            Surface(
                onClick = { onMoodSelected(mood) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) GoldPrimary else BgCard,
                border = BorderStroke(1.dp, if (isSelected) GoldLight else GoldPrimary.copy(alpha = 0.25f)),
                shadowElevation = if (isSelected) 6.dp else 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(text = mood.emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = mood.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BgBlack else TextPrimary
                        )
                    )
                }
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
                "Search web songs, Tamil movies, artists...",
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
