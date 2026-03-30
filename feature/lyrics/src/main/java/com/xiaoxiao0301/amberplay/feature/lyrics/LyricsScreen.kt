package com.xiaoxiao0301.amberplay.feature.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoxiao0301.amberplay.core.common.theme.Background
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.domain.model.LyricLine

@Composable
fun LyricsScreen(
    onClose: () -> Unit = {},
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val currentIdx   by viewModel.currentLineIndex.collectAsStateWithLifecycle()
    val playback     by viewModel.playbackState.collectAsStateWithLifecycle()
    val mode         by viewModel.lyricMode.collectAsStateWithLifecycle()
    val listState     = rememberLazyListState()

    // 自动滚动到当前行（保持居中偏上）
    LaunchedEffect(currentIdx) {
        listState.animateScrollToItem(
            index  = (currentIdx - 3).coerceAtLeast(0),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 80.dp, vertical = 32.dp),
    ) {
        // ─── 顶栏：歌名 + 模式切换 + 关闭 ──────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text       = playback.currentSong?.name ?: "",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text     = playback.currentSong?.artistText ?: "",
                    fontSize = 16.sp,
                    color    = OnSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeChip(
                    label    = when (mode) {
                        LyricMode.ORIGINAL    -> "原文"
                        LyricMode.TRANSLATION -> "译文"
                        LyricMode.BILINGUAL   -> "双语"
                    },
                    onClick  = { viewModel.cycleMode() },
                )
                ModeChip(label = "✕", onClick = onClose)
            }
        }

        Spacer(Modifier.height(24.dp))

        // ─── 歌词内容区 ──────────────────────────────────────────
        when (val state = uiState) {
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
                    Text("歌词加载失败：${state.msg}", color = MaterialTheme.colorScheme.error)
                }
            }
            is LyricsUiState.Ready -> {
                LazyColumn(
                    state               = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(state.lines) { idx, line ->
                        LyricLineItem(
                            line      = line,
                            mode      = state.mode,
                            isActive  = idx == currentIdx,
                            onClick   = { viewModel.seekToLine(line.timestampMs) },
                        )
                    }
                    item { Spacer(Modifier.height(200.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: LyricLine,
    mode: LyricMode,
    isActive: Boolean,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val textColor by animateColorAsState(
        targetValue = if (isActive) Purple else OnSurfaceVariant,
        animationSpec = tween(300),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Surface.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (mode != LyricMode.TRANSLATION) {
            Text(
                text       = line.text,
                fontSize   = if (isActive) 24.sp else 18.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color      = textColor,
                textAlign  = TextAlign.Center,
            )
        }
        if (mode != LyricMode.ORIGINAL && line.translation?.isNotBlank() == true) {
            Text(
                text      = line.translation ?: "",
                fontSize  = if (isActive) 18.sp else 14.sp,
                color     = textColor.copy(alpha = if (isActive) 0.9f else 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, onClick: () -> Unit) {
    Text(
        text     = label,
        fontSize = 15.sp,
        color    = Purple,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Purple.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

