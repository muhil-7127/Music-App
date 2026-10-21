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

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val authState by authViewModel.uiState.collectAsState()
    val musicUiState by musicViewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

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
                    AsyncImage(
                        model = "android.resource://com.muhil.music/drawable/muhil_logo",
                        contentDescription = "Logo",
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Muhil Music", style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                    ))
                    Text("Version 1.1.0 • Tamil-First Edition", style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
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
            SettingsSection(title = "Account") {
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
                SettingsItem(icon = Icons.Filled.MusicNote, label = "Streaming Mode", value = "Audius Full Track + Previews")
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
