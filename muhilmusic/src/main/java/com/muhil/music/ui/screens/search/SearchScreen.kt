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
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val tabs = remember { listOf("All", "Songs", "Artists", "Albums") }

    val trendingPills = remember {
        listOf("Taylor Swift", "Coldplay", "The Weeknd", "A.R. Rahman", "Billie Eilish", "Anirudh", "Ed Sheeran", "Dua Lipa")
    }

    LaunchedEffect(query) {
        viewModel.search(query)
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MuhilTopBar(title = "Search Web Music", onBack = { navController.popBackStack() })

            // Search input field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search songs, artists, albums online...", color = TextTertiary) },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
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
                // Discover Trending Searches
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        "Trending Searches",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(14.dp))
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
                    Spacer(Modifier.height(36.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.TravelExplore,
                                contentDescription = null,
                                tint = GoldPrimary.copy(alpha = 0.35f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Search millions of tracks available on the web",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
                            )
                        }
                    }
                }
            } else if (uiState.isSearching) {
                // Loading spinner
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Spacer(Modifier.height(14.dp))
                        Text("Searching web tracks...", style = MaterialTheme.typography.bodyMedium.copy(color = SilverMid))
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
                // Results based on tab
                when (selectedTab) {
                    0 -> { // All (Songs, Artists, Albums)
                        if (uiState.searchResults.isEmpty() && uiState.searchArtists.isEmpty() && uiState.searchAlbums.isEmpty()) {
                            EmptySearchResults(query = query)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                if (uiState.searchResults.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Songs (${uiState.searchResults.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold, color = TextPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
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
                                            onFavoriteClick = { viewModel.toggleFavorite(song.id) }
                                        )
                                        GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
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
                    1 -> { // Songs
                        if (uiState.searchResults.isEmpty()) {
                            EmptySearchResults(query = query)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(uiState.searchResults, key = { it.id }) { song ->
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
                    2 -> { // Artists
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
                    3 -> { // Albums
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
            Text("Try searching for another song title, artist, or album", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
        }
    }
}
