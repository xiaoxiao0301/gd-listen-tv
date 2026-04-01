package com.xiaoxiao0301.amberplay.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.core.media.PlayMode

/**
 * 底部迷你播放控制栏 — QQ Music 桌面版风格
 *
 * 布局：[封面] [歌名/歌手 | 进度条] [⏮ ⏸/▶ ⏭] [播放模式] [展开↗]
 */
@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val song  = state.currentSong ?: return  // 没有歌曲时不显示

    Column(
        modifier = modifier.fillMaxWidth().background(Surface)
    ) {
        // ── 顶部进度条 ────────────────────────────────────────────
        if (state.durationMs > 0) {
            LinearProgressIndicator(
                progress   = { state.positionMs.toFloat() / state.durationMs.toFloat() },
                modifier   = Modifier.fillMaxWidth().height(2.dp),
                color      = Purple,
                trackColor = Color.Transparent,
            )
        }

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 封面（点击跳转播放页） ────────────────────────────
            AsyncImage(
                model              = song.picUrl(),
                contentDescription = song.name,
                modifier           = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariant)
                    .clickable(onClick = onExpand),
            )

            // ── 歌名 + 歌手 ───────────────────────────────────────
            Column(Modifier.weight(1f)) {
                Text(
                    text       = song.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = song.artistText,
                    fontSize = 12.sp,
                    color    = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // ── 播放控制 ──────────────────────────────────────────
            MiniCtrlBtn("⏮") { viewModel.skipPrevious() }
            MiniCtrlBtn(
                label     = if (state.isPlaying) "⏸" else "▶",
                isPrimary = true,
            ) { viewModel.playOrPause() }
            MiniCtrlBtn("⏭") { viewModel.skipNext() }

            // ── 播放模式切换 ──────────────────────────────────────
            val modeLabel = when (state.playMode) {
                PlayMode.SEQUENTIAL -> "顺序"
                PlayMode.REPEAT_ALL -> "循环"
                PlayMode.REPEAT_ONE -> "单曲"
                PlayMode.SHUFFLE    -> "随机"
            }
            val modeActive = state.playMode == PlayMode.SHUFFLE
            MiniCtrlBtn(modeLabel, isActive = modeActive) { viewModel.cyclePlayMode() }

            // ── 展开到全屏播放页 ──────────────────────────────────
            MiniCtrlBtn("⤢") { onExpand() }
        }
    }
}

@Composable
private fun MiniCtrlBtn(
    label: String,
    isPrimary: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isPrimary -> Purple
                    isActive  -> Purple.copy(alpha = 0.25f)
                    focused   -> SurfaceVariant
                    else      -> Color.Transparent
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text      = label,
            fontSize  = if (isPrimary) 22.sp else 18.sp,
            color     = if (isPrimary || isActive) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

