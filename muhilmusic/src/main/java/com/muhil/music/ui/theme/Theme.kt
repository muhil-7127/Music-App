package com.muhil.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GoldPrimary    = Color(0xFFD4A017)
val GoldLight      = Color(0xFFF0C040)
val GoldDark       = Color(0xFFA07810)
val GoldShimmer    = Color(0xFFFFD700)
val SilverLight    = Color(0xFFE8E8E8)
val SilverMid      = Color(0xFFC0C0C0)
val SilverDark     = Color(0xFF8A8A8A)
val BgBlack        = Color(0xFF0A0A0F)
val BgDeepNavy     = Color(0xFF0D1117)
val BgSurface      = Color(0xFF161B22)
val BgCard         = Color(0xFF1C2128)
val BgElevated     = Color(0xFF21262D)
val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xFFC0C0C0)
val TextTertiary   = Color(0xFF8A8A8A)
val ErrorRed       = Color(0xFFCF6679)

private val MuhilDarkColorScheme = darkColorScheme(
    primary          = GoldPrimary,
    onPrimary        = BgBlack,
    primaryContainer = GoldDark,
    secondary        = SilverMid,
    onSecondary      = BgBlack,
    tertiary         = GoldLight,
    background       = BgBlack,
    onBackground     = TextPrimary,
    surface          = BgSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BgCard,
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    outline          = TextTertiary
)

@Composable
fun MuhilMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuhilDarkColorScheme,
        content = content
    )
}
