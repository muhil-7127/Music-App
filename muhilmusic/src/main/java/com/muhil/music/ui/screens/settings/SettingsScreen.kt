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
import com.muhil.music.ui.theme.*

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by authViewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        containerColor = BgBlack,
        topBar = { MuhilTopBar(title = "Settings", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // Profile section
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
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
                }
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
            SettingsSection(title = "Playback") {
                SettingsItem(icon = Icons.Filled.MusicNote, label = "Audio Quality", value = "High")
                SettingsItem(icon = Icons.Filled.LibraryMusic, label = "Library Path", value = "All External Storage")
            }

            Spacer(Modifier.height(8.dp))

            // About
            SettingsSection(title = "About") {
                SettingsItem(icon = Icons.Filled.Info, label = "App Name", value = "Muhil Music")
                SettingsItem(icon = Icons.Filled.Android, label = "Package", value = "com.muhil.music")
                SettingsItem(icon = Icons.Filled.Code, label = "Technology", value = "Kotlin + Jetpack Compose")
            }

            Spacer(Modifier.height(24.dp))

            if (uiState.isLoggedIn) {
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



