package com.muhil.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.muhil.music.domain.model.Album
import com.muhil.music.domain.model.Artist
import com.muhil.music.domain.model.Song
import com.muhil.music.ui.theme.*

@Composable
fun AlbumArtImage(
    uri: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(BgCard),
        contentAlignment = Alignment.Center
    ) {
        // Immediate subtle placeholder so view is never blank
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = GoldPrimary.copy(alpha = 0.3f),
            modifier = Modifier.size(28.dp)
        )
        if (!uri.isNullOrBlank()) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun WebPreviewBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = GoldPrimary.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.6f))
    ) {
        Text(
            text = "PREVIEW",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun FullTrackBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF10B981).copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.7f))
    ) {
        Text(
            text = "FULL TRACK",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    isFavorite: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            uri = song.albumArtUri,
            modifier = Modifier.size(52.dp),
            cornerRadius = 8.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isPlaying) GoldPrimary else TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (song.isPreview) {
                    Spacer(Modifier.width(6.dp))
                    WebPreviewBadge()
                } else if (song.isOnline) {
                    Spacer(Modifier.width(6.dp))
                    FullTrackBadge()
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onFavoriteClick != null) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) GoldPrimary else TextTertiary
                )
            }
        }
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun FeaturedSongCard(
    song: Song,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, if (isPlaying) GoldPrimary else GoldPrimary.copy(alpha = 0.2f)),
        modifier = modifier.width(148.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AlbumArtImage(
                    uri = song.albumArtUri,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 12.dp
                )
                if (song.isPreview) {
                    WebPreviewBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    )
                } else if (song.isOnline) {
                    FullTrackBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 6.dp)
                        .background(GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = BgBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) GoldPrimary else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistCircleCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(88.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape)
                .background(BgCard),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.artUri.isNullOrBlank()) {
                AsyncImage(
                    model = artist.artUri,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumSquareCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = BgCard,
        shadowElevation = 6.dp,
        border = BorderStroke(0.8.dp, SilverDark.copy(alpha = 0.25f)),
        modifier = modifier.width(135.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AlbumArtImage(
                uri = album.artUri,
                modifier = Modifier
                    .size(115.dp)
                    .clip(RoundedCornerShape(10.dp)),
                cornerRadius = 10.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = GoldPrimary.copy(alpha = 0.15f),
        thickness = 0.5.dp
    )
}

@Composable
fun MuhilTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = GoldPrimary)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BgDeepNavy,
            scrolledContainerColor = BgSurface
        )
    )
}

@Composable
fun MiniNowPlayingBar(
    song: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = BgCard,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(
                GoldPrimary.copy(alpha = glowAlpha),
                GoldPrimary.copy(alpha = 0.15f),
                GoldPrimary.copy(alpha = glowAlpha)
            ))
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtImage(
                uri = song.albumArtUri,
                modifier = Modifier.size(48.dp),
                cornerRadius = 10.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.isPreview) {
                        Spacer(Modifier.width(6.dp))
                        WebPreviewBadge()
                    } else if (song.isOnline) {
                        Spacer(Modifier.width(6.dp))
                        FullTrackBadge()
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = GoldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = SilverMid, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun GoldGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = BgBlack
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight)),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BgBlack
                )
            )
        }
    }
}
