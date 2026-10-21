package com.muhil.music.ui.screens.equalizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.muhil.music.media.AudioEqualizerManager
import com.muhil.music.ui.components.GoldDivider
import com.muhil.music.ui.components.MuhilTopBar
import com.muhil.music.ui.screens.home.MusicViewModel
import com.muhil.music.ui.theme.*

@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    navController: NavController
) {
    val eqState by viewModel.equalizerState.collectAsState()

    Scaffold(
        containerColor = BgBlack,
        topBar = {
            MuhilTopBar(
                title = "Audio Equalizer",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Master Toggle Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, if (eqState.isEnabled) GoldPrimary.copy(alpha = 0.5f) else SilverDark.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (eqState.isEnabled) GoldPrimary else SilverDark.copy(alpha = 0.3f),
                            contentColor = if (eqState.isEnabled) BgBlack else TextPrimary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Equalizer & Effects",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = if (eqState.isEnabled) "Custom Audio Processing Active" else "Disabled (Direct Output)",
                                style = MaterialTheme.typography.bodySmall.copy(color = if (eqState.isEnabled) GoldPrimary else TextTertiary)
                            )
                        }
                    }

                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgBlack,
                            checkedTrackColor = GoldPrimary,
                            uncheckedThumbColor = SilverMid,
                            uncheckedTrackColor = BgElevated
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!eqState.isSupported) {
                // Device Unsupported Notice
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgElevated),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = GoldPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Direct Audio Path", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Hardware Equalizer effects are not available on this specific audio routing session. Playback continues in pristine high-definition direct stereo audio.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // Presets Row
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(8.dp))

            val presets = AudioEqualizerManager.PRESETS.keys.toList()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = eqState.currentPreset.equals(preset, ignoreCase = true)
                    Surface(
                        onClick = {
                            if (!eqState.isEnabled) viewModel.setEqualizerEnabled(true)
                            viewModel.setEqualizerPreset(preset)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) GoldPrimary else BgCard,
                        border = BorderStroke(1.dp, if (isSelected) GoldLight else GoldPrimary.copy(alpha = 0.25f)),
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BgBlack else TextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bass Boost Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = BorderStroke(1.dp, SilverDark.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Speaker, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Bass Boost",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Text(
                            text = "${(eqState.bassBoostStrength / 10)}%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = eqState.bassBoostStrength.toFloat(),
                        onValueChange = {
                            if (!eqState.isEnabled) viewModel.setEqualizerEnabled(true)
                            viewModel.setBassBoost(it.toInt())
                        },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldPrimary,
                            activeTrackColor = GoldPrimary,
                            inactiveTrackColor = SilverDark.copy(alpha = 0.3f)
                        ),
                        enabled = eqState.isEnabled
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Frequency Bands
            Text(
                text = "FREQUENCY BANDS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(12.dp))

            eqState.bands.forEach { band ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = BorderStroke(0.5.dp, SilverDark.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = band.formattedFreq,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            val db = band.levelDb
                            val formattedDb = if (db >= 0) "+%.1f dB".format(db) else "%.1f dB".format(db)
                            Text(
                                text = formattedDb,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (db > 0) GoldPrimary else if (db < 0) SilverMid else TextSecondary
                                )
                            )
                        }

                        Slider(
                            value = band.levelMb.toFloat(),
                            onValueChange = { newMb ->
                                if (!eqState.isEnabled) viewModel.setEqualizerEnabled(true)
                                viewModel.setEqualizerBandLevel(band.bandIndex, newMb.toInt().toShort())
                            },
                            valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = SilverDark.copy(alpha = 0.3f)
                            ),
                            enabled = eqState.isEnabled
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetEqualizer() },
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset to Flat")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
