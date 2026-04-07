package com.xiaoxiao0301.amberplay.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.Amber
import com.xiaoxiao0301.amberplay.core.common.theme.AmberAccent
import com.xiaoxiao0301.amberplay.core.common.theme.AmberContainer
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurface
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerHigh
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerLow
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.domain.model.LyricLine
import com.xiaoxiao0301.amberplay.feature.lyrics.LyricsUiState
import com.xiaoxiao0301.amberplay.feature.lyrics.LyricsViewModel
import kotlin.math.abs

private val SLEEP_OPTIONS = listOf(0, 15, 30, 45, 60)
private val SLEEP_LABELS = listOf("关闭", "15 分钟", "30 分钟", "45 分钟", "60 分钟")

@Composable
fun PlayerScreen(
    onClose: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val song = state.currentSong

    val lyricUiState by lyricsViewModel.uiState.collectAsStateWithLifecycle()
    val lyricMode by lyricsViewModel.lyricMode.collectAsStateWithLifecycle()
    val currentLineIdx by lyricsViewModel.currentLineIndex.collectAsStateWithLifecycle()
    val lyricListState = rememberLazyListState()

    var showSleepDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(currentLineIdx, lyricUiState) {
        val ready = lyricUiState as? LyricsUiState.Ready ?: return@LaunchedEffect
        if (ready.lines.isNotEmpty()) {
            val target = currentLineIdx.coerceAtLeast(0)
            if (abs(target - lyricListState.firstVisibleItemIndex) >= 1) {
                lyricListState.animateScrollToItem(target)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.playerError.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeftPlayerPanel(
                modifier = Modifier.weight(0.5f),
                songTitle = song?.name ?: "未在播放",
                artist = song?.artistText ?: "",
                coverUrl = song?.picUrl(600),
                isPlaying = state.isPlaying,
                progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs.toFloat() else 0f,
                position = formatMs(state.positionMs),
                duration = formatMs(state.durationMs),
                onSeek = { fraction -> viewModel.seekTo((fraction * state.durationMs).toLong()) },
                onPrev = { viewModel.skipPrevious() },
                onPlayPause = { viewModel.playOrPause() },
                onNext = { viewModel.skipNext() },
                onShuffle = { viewModel.cyclePlayMode() },
                onRepeat = { viewModel.cyclePlayMode() },
                onQueue = onOpenQueue,
                onClose = onClose,
                onSleep = { showSleepDialog = true },
            )

            RightLyricsPanel(
                modifier = Modifier.weight(0.5f),
                uiState = lyricUiState,
                lyricMode = lyricMode,
                currentLineIdx = currentLineIdx,
                listState = lyricListState,
                onToggleMode = { lyricsViewModel.cycleMode() },
                onSeekToLine = { lyricsViewModel.seekToLine(it) },
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("睡眠定时") },
            text = {
                Column {
                    SLEEP_OPTIONS.forEachIndexed { index, minutes ->
                        TextButton(
                            onClick = {
                                viewModel.setSleepTimer(minutes)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(SLEEP_LABELS[index], fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun LeftPlayerPanel(
    modifier: Modifier,
    songTitle: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    position: String,
    duration: String,
    onSeek: (Float) -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit,
    onClose: () -> Unit,
    onSleep: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 34.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(456.dp)
                .clip(RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(500.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AmberContainer.copy(alpha = 0.58f), Color.Transparent),
                        ),
                    ),
            )
            AsyncImage(
                model = coverUrl,
                contentDescription = songTitle,
                modifier = Modifier
                    .size(456.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(SurfaceContainerHigh),
            )
        }

        Spacer(Modifier.height(30.dp))

        Text(
            text = songTitle,
            color = OnSurface,
            fontSize = 50.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist,
            color = OnSurfaceVariant,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "正在播放",
            color = OnSurfaceVariant.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(18.dp))

        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            colors = SliderDefaults.colors(
                thumbColor = AmberAccent,
                activeTrackColor = AmberAccent,
                inactiveTrackColor = AmberContainer,
            ),
            modifier = Modifier.fillMaxWidth(0.88f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.86f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(position, color = OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(duration, color = OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(22.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerBtn(Icons.Filled.Shuffle, 54.dp, onClick = onShuffle)
            PlayerBtn(Icons.Filled.SkipPrevious, 70.dp, onClick = onPrev)
            PlayerBtn(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 102.dp, primary = true, onClick = onPlayPause)
            PlayerBtn(Icons.Filled.SkipNext, 70.dp, onClick = onNext)
            PlayerBtn(Icons.Filled.Repeat, 54.dp, onClick = onRepeat)
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UtilityBtn("队列", onQueue)
            UtilityBtn("定时", onSleep)
            UtilityBtn("关闭", onClose)
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun RightLyricsPanel(
    modifier: Modifier,
    uiState: LyricsUiState,
    lyricMode: LyricMode,
    currentLineIdx: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onToggleMode: () -> Unit,
    onSeekToLine: (Long) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.56f))
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 30.dp, vertical = 26.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("歌词", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            val modeLabel = when (lyricMode) {
                LyricMode.ORIGINAL -> "原文"
                LyricMode.TRANSLATION -> "译文"
                LyricMode.BILINGUAL -> "双语"
            }
            Text(
                modeLabel,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(AmberContainer)
                    .clickable(onClick = onToggleMode)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        when (uiState) {
            LyricsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Amber)
                }
            }

            LyricsUiState.NoLyric -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无歌词", color = OnSurfaceVariant, fontSize = 24.sp)
                }
            }

            is LyricsUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("歌词加载失败", color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
                }
            }

            is LyricsUiState.Ready -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 104.dp, bottom = 188.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    itemsIndexed(uiState.lines) { index, line ->
                        LyricLineItem(
                            line = line,
                            mode = uiState.mode,
                            active = index == currentLineIdx,
                            upcoming = index > currentLineIdx,
                            onClick = { onSeekToLine(line.timestampMs) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: LyricLine,
    mode: LyricMode,
    active: Boolean,
    upcoming: Boolean,
    onClick: () -> Unit,
) {
    val color = when {
        active -> AmberAccent
        upcoming -> OnSurfaceVariant.copy(alpha = 0.62f)
        else -> OnSurfaceVariant.copy(alpha = 0.38f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(if (active) 50.dp else 0.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) Amber else Color.Transparent),
        )

        Spacer(Modifier.width(12.dp))

        Column {
            if (mode != LyricMode.TRANSLATION) {
                Text(
                    text = line.text,
                    color = color,
                    fontSize = if (active) 46.sp else 29.sp,
                    lineHeight = if (active) 52.sp else 35.sp,
                    fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
                )
            }
            if (mode != LyricMode.ORIGINAL && !line.translation.isNullOrBlank()) {
                Text(
                    text = line.translation ?: "",
                    color = color.copy(alpha = 0.72f),
                    fontSize = if (active) 21.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PlayerBtn(
    icon: ImageVector,
    size: Dp,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = if (primary) 0.dp else 1.dp,
                color = OnSurfaceVariant.copy(alpha = 0.16f),
                shape = CircleShape,
            )
            .background(if (primary) Amber else SurfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) Color.White else OnSurface,
            modifier = Modifier.size((size.value * if (primary) 0.40f else 0.34f).dp),
        )
    }
}

@Composable
private fun UtilityBtn(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = label,
        color = OnSurface,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) AmberContainer else SurfaceContainerHigh)
            .border(
                width = if (focused) 1.2.dp else 1.dp,
                color = if (focused) Amber.copy(alpha = 0.28f) else OnSurfaceVariant.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
    )
}

private fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
