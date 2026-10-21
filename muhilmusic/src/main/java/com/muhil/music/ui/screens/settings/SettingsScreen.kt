package com.muhil.music.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.muhil.music.ui.components.GoldGradientButton
import com.muhil.music.ui.components.GoldDivider
import com.muhil.music.ui.components.MuhilTopBar
import com.muhil.music.ui.screens.auth.AuthViewModel
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import java.io.File
import timber.log.Timber

private val avatarPresets = listOf(
    Pair("Muhil Gold", "android.resource://com.muhil.music/drawable/muhil_logo"),
    Pair("Vibe Vinyl", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&auto=format&fit=crop&q=80"),
    Pair("Neon Stage", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80"),
    Pair("DJ Beats", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80"),
    Pair("Acoustic Melody", "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=400&auto=format&fit=crop&q=80")
)

private fun saveAvatarToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val file = File(context.filesDir, "user_profile_avatar.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        Timber.e(e, "Failed to save profile picture")
        null
    }
}

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    val musicUiState by musicViewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveAvatarToInternalStorage(context, uri)
            if (savedPath != null) {
                musicViewModel.setProfilePicture(savedPath)
            }
        }
    }

    val languages = listOf("Tamil", "English", "Hindi", "All")

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = BgCard,
            title = { Text("Sign Out", style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)) },
            text = { Text("Are you sure you want to sign out?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.signOut()
                    showSignOutDialog = false
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }) {
                    Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = GoldPrimary)
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = BgCard,
            title = {
                Text(
                    "Preferred Language",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        "Select your preferred language. Muhil Music will prioritize songs, soundtracks, and recommendations in this language.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                    )
                    Spacer(Modifier.height(16.dp))
                    languages.forEach { lang ->
                        val isSelected = musicUiState.preferredLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    musicViewModel.setPreferredLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    musicViewModel.setPreferredLanguage(lang)
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (lang == "Tamil") "Tamil (தமிழ் - First Class)" else lang,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isSelected) GoldPrimary else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = GoldPrimary)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = BgCard,
            title = { Text("Clear Search History", style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)) },
            text = { Text("Are you sure you want to clear all your recent searches?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    musicViewModel.clearSearchHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = GoldPrimary)
                }
            }
        )
    }

    if (showProfileDialog) {
        var tempName by remember(musicUiState.userName) { mutableStateOf(musicUiState.userName) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = BgCard,
            title = {
                Text(
                    "Profile & Identity",
                    style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.dp, GoldPrimary, CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = musicUiState.profilePictureUri ?: "android.resource://com.muhil.music/drawable/muhil_logo",
                            contentDescription = "Avatar Preview",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                        Surface(
                            shape = CircleShape,
                            color = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Pick Image",
                                tint = BgBlack,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        border = BorderStroke(1.dp, GoldPrimary),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose from Gallery / Photos")
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Or Choose a Preset Avatar",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(avatarPresets) { (name, url) ->
                            val isSelected = musicUiState.profilePictureUri == url
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { musicViewModel.setProfilePicture(url) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) GoldPrimary else SilverMid.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) GoldPrimary else TextTertiary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SilverMid.copy(alpha = 0.5f),
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextTertiary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (musicUiState.profilePictureUri != null) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { musicViewModel.setProfilePicture(null) }) {
                            Text("Reset Photo to Default", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                GoldGradientButton(
                    text = "Save",
                    onClick = {
                        musicViewModel.setUserName(tempName)
                        showProfileDialog = false
                    },
                    modifier = Modifier.height(40.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = GoldPrimary)
                }
            }
        )
    }

    Scaffold(
        containerColor = BgBlack,
        topBar = { MuhilTopBar(title = "Settings", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // Profile banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(2.dp, GoldPrimary, CircleShape)
                            .clickable { showProfileDialog = true },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        AsyncImage(
                            model = musicUiState.profilePictureUri ?: "android.resource://com.muhil.music/drawable/muhil_logo",
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                        Surface(
                            shape = CircleShape,
                            color = GoldPrimary,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Profile Picture",
                                tint = BgBlack,
                                modifier = Modifier.padding(5.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = musicUiState.userName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                        )
                    )
                    Text(
                        text = "Tap to set photo & name • v1.1.0",
                        style = MaterialTheme.typography.bodySmall.copy(color = SilverMid)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Music Discovery & Personalization Section
            SettingsSection(title = "Discovery & Personalization") {
                SettingsClickableItem(
                    icon = Icons.Filled.Language,
                    label = "Preferred Music Language",
                    value = if (musicUiState.preferredLanguage == "Tamil") "Tamil (தமிழ்)" else musicUiState.preferredLanguage,
                    onClick = { showLanguageDialog = true }
                )
                SettingsClickableItem(
                    icon = Icons.Filled.History,
                    label = "Search History",
                    value = "${musicUiState.searchHistory.size} saved • Tap to clear",
                    onClick = { showClearHistoryDialog = true }
                )
                SettingsItem(
                    icon = Icons.Filled.Recommend,
                    label = "Recommendation Engine",
                    value = "Adaptive Hybrid (Taste + History)"
                )
            }

            Spacer(Modifier.height(8.dp))

            // Account section
            SettingsSection(title = "Profile & Account") {
                SettingsClickableItem(
                    icon = Icons.Filled.AccountCircle,
                    label = "Profile Details",
                    value = "${musicUiState.userName} • Tap to edit",
                    onClick = { showProfileDialog = true }
                )
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    SettingsItem(icon = Icons.Filled.Email, label = "Email", value = user.email ?: "Not set")
                    SettingsItem(icon = Icons.Filled.Person, label = "Phone", value = user.phoneNumber ?: "Not set")
                    SettingsItem(icon = Icons.Filled.VerifiedUser, label = "Email Verified",
                        value = if (user.isEmailVerified) "Yes" else "No")
                } else {
                    SettingsItem(icon = Icons.Filled.AccountCircle, label = "Status", value = "Not signed in")
                }
            }

            Spacer(Modifier.height(8.dp))

            // App section
            SettingsSection(title = "Playback & Streaming") {
                SettingsItem(icon = Icons.Filled.MusicNote, label = "Streaming Mode", value = "100% Full Track Streaming (JioSaavn + Audius)")
                SettingsItem(icon = Icons.Filled.LibraryMusic, label = "Library Path", value = "All External Storage")
            }

            Spacer(Modifier.height(8.dp))

            // About
            SettingsSection(title = "About") {
                SettingsItem(icon = Icons.Filled.Info, label = "App Name", value = "Muhil Music")
                SettingsItem(icon = Icons.Filled.Android, label = "Package", value = "com.muhil.music")
                SettingsItem(icon = Icons.Filled.Code, label = "Technology", value = "Kotlin + Jetpack Compose + Media3")
            }

            Spacer(Modifier.height(24.dp))

            if (authState.isLoggedIn) {
                GoldGradientButton(
                    text = "Sign Out",
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(color = GoldPrimary, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Surface(color = BgCard, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
    }
    GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun SettingsClickableItem(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = GoldLight))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = SilverMid, modifier = Modifier.size(18.dp))
    }
    GoldDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
