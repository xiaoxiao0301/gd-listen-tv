package com.xiaoxiao0301.amberplay.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.Background
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.core.media.PlayMode
import com.xiaoxiao0301.amberplay.domain.model.LyricLine
import com.xiaoxiao0301.amberplay.feature.lyrics.LyricsUiState
import com.xiaoxiao0301.amberplay.feature.lyrics.LyricsViewModel

private val SLEEP_OPTIONS = listOf(0, 15, 30, 45, 60)
private val SLEEP_LABELS  = listOf("关闭", "15 分钟", "30 分钟", "45 分钟", "60 分钟")

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

    LaunchedEffect(currentLineIdx, lyricUiState) {
        val ready = lyricUiState as? LyricsUiState.Ready ?: return@LaunchedEffect
        if (ready.lines.isNotEmpty()) {
            lyricListState.animateScrollToItem((currentLineIdx - 3).coerceAtLeast(0))
        }
    }

    var showSleepDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val rotationTransition = rememberInfiniteTransition(label = "cover_rotation")
    val coverRotation by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cover_rotation_value",
    )

    LaunchedEffect(Unit) {
        viewModel.playerError.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(36.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 左栏：专辑封面 + 控制模块
            Column(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = song?.picUrl(600),
                    contentDescription = song?.name,
                    modifier = Modifier
                        .size(320.dp)
                        .graphicsLayer { rotationZ = if (state.isPlaying) coverRotation else 0f }
                        .clip(RoundedCornerShape(160.dp))
                        .background(Surface),
                )

                Text(
                    text = song?.name ?: "未在播放",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = song?.artistText ?: "",
                    fontSize = 18.sp,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                )

                Column(Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (state.durationMs > 0) {
                            state.positionMs.toFloat() / state.durationMs.toFloat()
                        } else 0f,
                        onValueChange = { fraction ->
                            viewModel.seekTo((fraction * state.durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Purple,
                            activeTrackColor = Purple,
                            inactiveTrackColor = Surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatMs(state.positionMs), fontSize = 14.sp, color = OnSurfaceVariant)
                        Text(formatMs(state.durationMs), fontSize = 14.sp, color = OnSurfaceVariant)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerIconButton("⏮", 52.dp) { viewModel.skipPrevious() }
                    PlayerIconButton(
                        label = if (state.isPlaying) "⏸" else "▶",
                        size = 72.dp,
                        isPrimary = true,
                    ) { viewModel.playOrPause() }
                    PlayerIconButton("⏭", 52.dp) { viewModel.skipNext() }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val modeLabel = when (state.playMode) {
                        PlayMode.SEQUENTIAL -> "顺序"
                        PlayMode.REPEAT_ALL -> "循环"
                        PlayMode.REPEAT_ONE -> "单曲"
                        PlayMode.SHUFFLE -> "随机"
                    }
                    PlayerIconButton(modeLabel, 44.dp) { viewModel.cyclePlayMode() }
                    PlayerIconButton("队列", 44.dp) { onOpenQueue() }

                    val speedLabel = when (state.speed) {
                        0.5f -> "0.5x"
                        0.75f -> "0.75x"
                        1.25f -> "1.25x"
                        1.5f -> "1.5x"
                        2.0f -> "2.0x"
                        else -> "1.0x"
                    }
                    PlayerIconButton(speedLabel, 44.dp) { viewModel.cycleSpeed() }
                    PlayerIconButton("⏰", 44.dp) { showSleepDialog = true }
                    PlayerIconButton("✕", 44.dp) { onClose() }
                }
            }

            // 右栏：歌词
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface.copy(alpha = 0.35f))
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "歌词",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    val modeLabel = when (lyricMode) {
                        LyricMode.ORIGINAL -> "原文"
                        LyricMode.TRANSLATION -> "译文"
                        LyricMode.BILINGUAL -> "双语"
                    }
                    Text(
                        text = modeLabel,
                        fontSize = 14.sp,
                        color = Purple,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Purple.copy(alpha = 0.15f))
                            .clickable { lyricsViewModel.cycleMode() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                when (val ui = lyricUiState) {
                    is LyricsUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Purple)
                        }
                    }
                    is LyricsUiState.NoLyric -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无歌词", color = OnSurfaceVariant, fontSize = 20.sp)
                        }
                    }
                    is LyricsUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("歌词加载失败", color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                        }
                    }
                    is LyricsUiState.Ready -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lyricListState,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(ui.lines) { index, line ->
                                LyricRow(
                                    line = line,
                                    mode = ui.mode,
                                    isActive = index == currentLineIdx,
                                    onClick = { lyricsViewModel.seekToLine(line.timestampMs) },
                                )
                            }
                            item { Spacer(Modifier.height(180.dp)) }
                        }
                    }
                }
            }
        }

    // ─── 睡眠定时对话框 ───────────────────────────────────────────
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title            = { Text("睡眠定时") },
            text             = {
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
                TextButton(onClick = { showSleepDialog = false }) { Text("取消") }
            },
        )
    }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    } // end Box
}

@Composable
private fun LyricRow(
    line: LyricLine,
    mode: LyricMode,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) Purple else OnSurfaceVariant,
        animationSpec = tween(250),
        label = "lyric_color",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) SurfaceVariant.copy(alpha = 0.7f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (mode != LyricMode.TRANSLATION) {
            Text(
                text = line.text,
                fontSize = if (isActive) 24.sp else 19.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }

        val translation = line.translation
        if (mode != LyricMode.ORIGINAL && !translation.isNullOrBlank()) {
            Text(
                text = translation,
                fontSize = if (isActive) 17.sp else 14.sp,
                color = textColor.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    label: String,
    size: Dp,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                when {
                    isPrimary -> Purple
                    focused   -> Surface
                    else      -> Color.Transparent
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = (size.value * 0.35f).sp, color = Color.White)
    }
}

private fun formatMs(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
