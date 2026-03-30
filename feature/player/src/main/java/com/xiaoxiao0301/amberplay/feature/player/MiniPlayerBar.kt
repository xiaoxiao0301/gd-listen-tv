package com.xiaoxiao0301.amberplay.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant

@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state   by viewModel.playbackState.collectAsStateWithLifecycle()
    val song     = state.currentSong ?: return  // 没有歌曲时不显示

    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (focused) SurfaceVariant else Surface)
            .clickable(onClick = onExpand)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        // 进度条（超薄）
        if (state.durationMs > 0) {
            LinearProgressIndicator(
                progress  = { state.positionMs.toFloat() / state.durationMs.toFloat() },
                modifier  = Modifier.fillMaxWidth().height(2.dp),
                color     = Purple,
                trackColor = Color.Transparent,
            )
        }

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 封面缩略图
            val picUrl = "https://music-api.gdstudio.xyz/api.php" +
                    "?types=pic&source=${song.source}&id=${song.picId}&size=300"
            AsyncImage(
                model              = picUrl,
                contentDescription = song.name,
                modifier           = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant),
            )

            // 歌名 + 歌手
            Column(Modifier.weight(1f)) {
                Text(
                    text       = song.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text     = song.artistText,
                    fontSize = 13.sp,
                    color    = OnSurfaceVariant,
                    maxLines = 1,
                )
            }

            // 控制按钮
            MiniButton("⏮") { viewModel.skipPrevious() }
            MiniButton(if (state.isPlaying) "⏸" else "▶") { viewModel.playOrPause() }
            MiniButton("⏭") { viewModel.skipNext() }
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    Text(
        text     = label,
        fontSize = 22.sp,
        color    = Color.White,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}
