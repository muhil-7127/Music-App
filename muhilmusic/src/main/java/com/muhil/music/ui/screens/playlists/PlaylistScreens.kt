package com.muhil.music.ui.screens.playlists

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muhil.music.data.local.entity.PlaylistEntity
import com.muhil.music.data.repository.MusicRepository
import com.muhil.music.ui.components.*
import com.muhil.music.ui.navigation.Screen
import com.muhil.music.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }
}

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    navController: NavController
) {
    val playlists by viewModel.playlists.collectAsState()

    Scaffold(
        containerColor = BgBlack,
        topBar = {
            MuhilTopBar(
                title = "Playlists",
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.CreatePlaylist.route) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Create Playlist", tint = GoldPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreatePlaylist.route) },
                containerColor = GoldPrimary, contentColor = BgBlack
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Playlist")
            }
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.QueueMusic, null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Playlists Yet", style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                    Text("Create your first playlist", style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary), modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(20.dp))
                    GoldGradientButton(text = "Create Playlist",
                        onClick = { navController.navigate(Screen.CreatePlaylist.route) },
                        modifier = Modifier.padding(horizontal = 40.dp))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { /* TODO: open playlist */ }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp)
                                .background(BgCard, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.QueueMusic, null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold, color = TextPrimary),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Playlist", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                            Icon(Icons.Rounded.MoreVert, null, tint = TextTertiary)
                        }
                    }
                    GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    navController: NavController
) {
    var name by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(40.dp))
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = GoldPrimary)
            }
            Spacer(Modifier.height(24.dp))
            Text("Create Playlist", style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
            ))
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name", color = SilverDark) },
                leadingIcon = { Icon(Icons.Rounded.QueueMusic, null, tint = GoldPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = SilverDark.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = GoldPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))
            GoldGradientButton(
                text = "Create",
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createPlaylist(name)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
        }
    }
}
