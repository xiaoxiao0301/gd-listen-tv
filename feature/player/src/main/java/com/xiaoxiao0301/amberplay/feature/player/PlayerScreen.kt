package com.xiaoxiao0301.amberplay.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.text.font.FontWeight
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
import com.xiaoxiao0301.amberplay.core.media.PlayMode

private val SLEEP_OPTIONS = listOf(0, 15, 30, 45, 60)
private val SLEEP_LABELS  = listOf("关闭", "15 分钟", "30 分钟", "45 分钟", "60 分钟")

@Composable
fun PlayerScreen(
    onClose: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val song   = state.currentSong
    var showSleepDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.playerError.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Box(Modifier.fillMaxSize()) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(48.dp),
    ) {
        // ─── 左侧：专辑封面 ──────────────────────────────────────
        val picUrl = song?.let {
            "https://music-api.gdstudio.xyz/api.php" +
                    "?types=pic&source=${it.source}&id=${it.picId}&size=500"
        }
        AsyncImage(
            model              = picUrl,
            contentDescription = song?.name,
            modifier           = Modifier
                .fillMaxHeight()
                .weight(0.4f)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface),
        )

        Spacer(Modifier.width(40.dp))

        // ─── 右侧：信息 + 控制 ───────────────────────────────────
        Column(
            modifier            = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // 歌名 + 歌手
            Column {
                Text(
                    text       = song?.name ?: "未在播放",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "${song?.artistText ?: ""} · ${song?.album ?: ""}",
                    fontSize = 18.sp,
                    color    = OnSurfaceVariant,
                    maxLines = 1,
                )
            }

            // 进度条
            Column {
                Slider(
                    value         = if (state.durationMs > 0) {
                        state.positionMs.toFloat() / state.durationMs.toFloat()
                    } else 0f,
                    onValueChange = { fraction ->
                        viewModel.seekTo((fraction * state.durationMs).toLong())
                    },
                    colors        = SliderDefaults.colors(
                        thumbColor        = Purple,
                        activeTrackColor  = Purple,
                        inactiveTrackColor= Surface,
                    ),
                    modifier      = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMs(state.positionMs), fontSize = 14.sp, color = OnSurfaceVariant)
                    Text(formatMs(state.durationMs), fontSize = 14.sp, color = OnSurfaceVariant)
                }
            }

            // 控制按钮行
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                PlayerIconButton("⏮", 52.dp) { viewModel.skipPrevious() }
                PlayerIconButton(
                    label    = if (state.isPlaying) "⏸" else "▶",
                    size     = 72.dp,
                    isPrimary = true,
                    onClick  = { viewModel.playOrPause() }
                )
                PlayerIconButton("⏭", 52.dp) { viewModel.skipNext() }
            }

            // 功能按钮行（播放模式、歌词、速度、睡眠定时）
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val modeLabel = when (state.playMode) {
                    PlayMode.SEQUENTIAL -> "顺序"
                    PlayMode.REPEAT_ALL -> "循环全部"
                    PlayMode.REPEAT_ONE -> "单曲循环"
                    PlayMode.SHUFFLE    -> "随机"
                }
                PlayerIconButton(modeLabel, 48.dp) { viewModel.cyclePlayMode() }
                PlayerIconButton("歌词", 48.dp) { onOpenLyrics() }
                // 播放速度
                val speedLabel = when (state.speed) {
                    0.75f  -> "0.75x"
                    1.25f  -> "1.25x"
                    1.5f   -> "1.5x"
                    2.0f   -> "2.0x"
                    else   -> "1.0x"
                }
                PlayerIconButton(speedLabel, 48.dp) { viewModel.cycleSpeed() }
                // 睡眠定时
                PlayerIconButton("⏰", 48.dp) { showSleepDialog = true }
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
        modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
    )
    } // end Box
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
            .focusable()
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
