package com.xiaoxiao0301.amberplay.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.Amber
import com.xiaoxiao0301.amberplay.core.common.theme.AmberContainer
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurface
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerHigh
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerLow
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl

@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val song = state.currentSong

    if (song == null) {
        Row(
            modifier = modifier
                .height(96.dp)
                .fillMaxWidth()
                .background(SurfaceContainerHigh)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("暂无播放", color = OnSurfaceVariant, fontSize = 16.sp)
        }
        return
    }

    Column(
        modifier = modifier
            .height(96.dp)
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.10f)),
    ) {
        val progress = if (state.durationMs > 0) {
            state.positionMs.toFloat() / state.durationMs.toFloat()
        } else 0f
        val positionText = formatMiniMs(state.positionMs)
        val durationText = formatMiniMs(state.durationMs)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onExpand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = song.picUrl(),
                    contentDescription = song.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.name,
                        color = OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artistText,
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniIconBtn(Icons.Filled.SkipPrevious) { viewModel.skipPrevious() }
                    Spacer(Modifier.width(8.dp))
                    MiniIconBtn(
                        icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        primary = true,
                    ) { viewModel.playOrPause() }
                    Spacer(Modifier.width(8.dp))
                    MiniIconBtn(Icons.Filled.SkipNext) { viewModel.skipNext() }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.74f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        positionText,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(AmberContainer),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Amber),
                        )
                    }
                    Text(
                        durationText,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniIconBtn(Icons.Filled.FavoriteBorder) { }
                Spacer(Modifier.width(8.dp))
                MiniIconBtn(Icons.AutoMirrored.Filled.QueueMusic) { onOpenQueue() }
                Spacer(Modifier.width(8.dp))
                MiniIconBtn(Icons.Filled.OpenInFull) { onExpand() }
            }
        }
    }
}

@Composable
private fun MiniIconBtn(
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(if (primary) 52.dp else 40.dp)
            .clip(CircleShape)
            .background(
                when {
                    primary -> Amber
                    focused -> AmberContainer
                    else -> Color.Transparent
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(if (primary) 28.dp else 20.dp),
        )
    }
}

private fun formatMiniMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
