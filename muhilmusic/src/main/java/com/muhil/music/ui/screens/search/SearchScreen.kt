package com.muhil.music.ui.screens.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muhil.music.domain.model.Song
import com.muhil.music.domain.search.MovieSoundtrack
import com.muhil.music.domain.search.QueryIntent
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
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val tabs = remember { listOf("All", "Songs", "Soundtracks", "Artists", "Albums") }

    val trendingPills = remember {
        listOf(
            "Leo", "Jailer", "Vikram", "Arabic Kuthu",
            "Anirudh", "A.R. Rahman", "Ilaiyaraaja", "Ponniyin Selvan",
            "Kaadhal", "Harris Jayaraj", "Taylor Swift", "Coldplay"
        )
    }

    LaunchedEffect(query) {
        viewModel.search(query)
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MuhilTopBar(title = "Search Muhil Music", onBack = { navController.popBackStack() })

            // Search input field with intent badge
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Song, movie (e.g. Leo), artist, Tamil lyrics...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = GoldPrimary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, null, tint = SilverDark)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = SilverDark.copy(alpha = 0.35f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GoldPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true
            )

            // Category filter chips
            if (query.isNotBlank()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tabs) { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) GoldPrimary else BgCard,
                            border = BorderStroke(1.dp, if (isSelected) GoldLight else GoldPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BgBlack else TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (query.isBlank()) {
                // Recent Searches & Trending Tamil Searches
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (uiState.searchHistory.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                        Text("Clear All", color = GoldPrimary, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.searchHistory.forEach { historyItem ->
                                        Surface(
                                            onClick = { query = historyItem },
                                            shape = RoundedCornerShape(20.dp),
                                            color = BgCard,
                                            border = BorderStroke(1.dp, SilverDark.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.History,
                                                    contentDescription = null,
                                                    tint = SilverMid,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    historyItem,
                                                    style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Remove",
                                                    tint = TextTertiary,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { viewModel.removeSearchHistoryItem(historyItem) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                "Popular & Tamil Searches",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                trendingPills.forEach { pill ->
                                    Surface(
                                        onClick = { query = pill },
                                        shape = RoundedCornerShape(20.dp),
                                        color = BgCard,
                                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.TrendingUp,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(pill, style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.TravelExplore,
                                    contentDescription = null,
                                    tint = GoldPrimary.copy(alpha = 0.35f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Search by song title, movie soundtrack, artist, or lyrics",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
                                )
                            }
                        }
                    }
                }
            } else if (uiState.isSearching) {
                // Loading spinner
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Spacer(Modifier.height(14.dp))
                        Text("Finding exact matches...", style = MaterialTheme.typography.bodyMedium.copy(color = SilverMid))
                    }
                }
            } else if (uiState.searchError != null && uiState.searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Search Connection Issue", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
                        Text(uiState.searchError ?: "", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.search(query) }) {
                            Text("Retry Search", color = GoldPrimary)
                        }
                    }
                }
            } else {
                // Results based on selected tab
                when (selectedTab) {
                    0 -> { // All
                        if (uiState.searchResults.isEmpty() && uiState.searchArtists.isEmpty() && uiState.searchAlbums.isEmpty()) {
                            EmptySearchResults(query = query)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                // 1. TOP EXACT MATCH HERO CARD
                                uiState.searchTopMatch?.let { topSong ->
                                    item {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            TopMatchHeroCard(
                                                song = topSong,
                                                isPlaying = playback.currentSong?.id == topSong.id && playback.isPlaying,
                                                isFavorite = topSong.id in uiState.favoriteSongIds,
                                                onPlayClick = {
                                                    viewModel.playSong(topSong)
                                                    navController.navigate(Screen.NowPlaying.route)
                                                },
                                                onFavoriteClick = { viewModel.toggleFavorite(topSong) },
                                                onQueueClick = { viewModel.addToQueue(topSong) }
                                            )
                                        }
                                    }
                                }

                                // 2. MOVIE SOUNDTRACK DEDICATED SECTION
                                uiState.searchMovieSoundtrack?.let { st ->
                                    item {
                                        SoundtrackCard(
                                            soundtrack = st,
                                            onPlaySoundtrack = {
                                                viewModel.playSongs(st.tracks, 0)
                                                navController.navigate(Screen.NowPlaying.route)
                                            },
                                            onSongClick = { song ->
                                                viewModel.playSong(song)
                                                navController.navigate(Screen.NowPlaying.route)
                                            }
                                        )
                                    }
                                }

                                // 3. SONGS LIST (remaining or all ranked)
                                if (uiState.searchResults.isNotEmpty()) {
                                    val otherSongs = if (uiState.searchTopMatch != null) {
                                        uiState.searchResults.filter { it.id != uiState.searchTopMatch?.id }
                                    } else {
                                        uiState.searchResults
                                    }

                                    if (otherSongs.isNotEmpty()) {
                                        item {
                                            Text(
                                                "More Songs (${otherSongs.size})",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold, color = TextPrimary
                                                ),
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                            )
                                        }
                                        items(otherSongs, key = { it.id }) { song ->
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

                                // 4. ARTISTS
                                if (uiState.searchArtists.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Artists (${uiState.searchArtists.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold, color = TextPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(uiState.searchArtists, key = { it.id }) { artist ->
                                                ArtistCircleCard(
                                                    artist = artist,
                                                    onClick = { query = artist.name; selectedTab = 1 }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(14.dp))
                                    }
                                }

                                // 5. ALBUMS
                                if (uiState.searchAlbums.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Albums (${uiState.searchAlbums.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold, color = TextPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            items(uiState.searchAlbums, key = { it.id }) { album ->
                                                AlbumSquareCard(
                                                    album = album,
                                                    onClick = { query = album.title; selectedTab = 1 }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Songs Tab
                        if (uiState.searchResults.isEmpty()) {
                            EmptySearchResults(query = query)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                uiState.searchTopMatch?.let { topSong ->
                                    item {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            TopMatchHeroCard(
                                                song = topSong,
                                                isPlaying = playback.currentSong?.id == topSong.id && playback.isPlaying,
                                                isFavorite = topSong.id in uiState.favoriteSongIds,
                                                onPlayClick = {
                                                    viewModel.playSong(topSong)
                                                    navController.navigate(Screen.NowPlaying.route)
                                                },
                                                onFavoriteClick = { viewModel.toggleFavorite(topSong) },
                                                onQueueClick = { viewModel.addToQueue(topSong) }
                                            )
                                        }
                                    }
                                }
                                items(uiState.searchResults, key = { it.id }) { song ->
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
                    2 -> { // Soundtracks Tab
                        val soundtracks = uiState.searchSoundtracks.ifEmpty {
                            listOfNotNull(uiState.searchMovieSoundtrack)
                        }
                        if (soundtracks.isEmpty()) {
                            EmptySearchResults(query = "Soundtracks for \"$query\"")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(soundtracks, key = { it.movieName }) { st ->
                                    SoundtrackCard(
                                        soundtrack = st,
                                        onPlaySoundtrack = {
                                            viewModel.playSongs(st.tracks, 0)
                                            navController.navigate(Screen.NowPlaying.route)
                                        },
                                        onSongClick = { song ->
                                            viewModel.playSong(song)
                                            navController.navigate(Screen.NowPlaying.route)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    3 -> { // Artists Tab
                        if (uiState.searchArtists.isEmpty()) {
                            EmptySearchResults(query = query)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.searchArtists, key = { it.id }) { artist ->
                                    ArtistCircleCard(
                                        artist = artist,
                                        onClick = { query = artist.name; selectedTab = 1 }
                                    )
                                }
                            }
                        }
                    }
                    4 -> { // Albums Tab
                        if (uiState.searchAlbums.isEmpty()) {
                            EmptySearchResults(query = query)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.searchAlbums, key = { it.id }) { album ->
                                    AlbumSquareCard(
                                        album = album,
                                        onClick = { query = album.title; selectedTab = 1 }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundtrackCard(
    soundtrack: MovieSoundtrack,
    onPlaySoundtrack: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    uri = soundtrack.albumArtUri,
                    modifier = Modifier.size(68.dp),
                    cornerRadius = 10.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GoldPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "MOVIE SOUNDTRACK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = soundtrack.movieName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${soundtrack.composer} • ${soundtrack.tracks.size} tracks",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPlaySoundtrack) {
                    Surface(
                        shape = CircleShape,
                        color = GoldPrimary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play Album",
                            tint = BgBlack,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            GoldDivider()

            // Preview tracks list
            soundtrack.tracks.take(4).forEachIndexed { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(track) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(color = GoldPrimary),
                        modifier = Modifier.width(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (track.isPreview) {
                        WebPreviewBadge()
                    } else if (track.isOnline) {
                        FullTrackBadge()
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchResults(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SearchOff, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(14.dp))
            Text("No results for \"$query\"", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
            Spacer(Modifier.height(6.dp))
            Text("Try searching for another song title, movie, artist, or album", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
        }
    }
}
