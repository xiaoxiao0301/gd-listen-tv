package com.xiaoxiao0301.amberplay.feature.queue

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun QueueScreen(
    onSongSelected: () -> Unit = {},
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val queue    by viewModel.queue.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentSongId = playback.currentSong?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "播放队列",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            if (queue.isNotEmpty()) {
                Text(
                    text     = "清空",
                    fontSize = 16.sp,
                    color    = Purple,
                    modifier = Modifier
                        .clickable { viewModel.clear() }
                        .padding(8.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("播放队列为空", color = OnSurfaceVariant, fontSize = 18.sp)
            }
        } else {
            // reorderIndex != null  →  该行处于"移动模式"，▲▼ 高亮可操作
            var reorderIndex by remember { mutableStateOf<Int?>(null) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(queue) { index, song ->
                    val isReordering = reorderIndex == index
                    QueueSongRow(
                        position     = index,
                        total        = queue.size,
                        song         = song,
                        isPlaying    = song.id == currentSongId,
                        isReordering = isReordering,
                        onClick      = {
                            if (reorderIndex != null) { reorderIndex = null }
                            else { viewModel.playAt(index); onSongSelected() }
                        },
                        onToggleReorder = { reorderIndex = if (isReordering) null else index },
                        onRemove     = { reorderIndex = null; viewModel.remove(index) },
                        onMoveUp     = {
                            viewModel.move(index, index - 1)
                            reorderIndex = index - 1
                        },
                        onMoveDown   = {
                            viewModel.move(index, index + 1)
                            reorderIndex = index + 1
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    position: Int,
    total: Int,
    song: Song,
    isPlaying: Boolean,
    isReordering: Boolean,
    onClick: () -> Unit,
    onToggleReorder: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val rowBg = when {
        isReordering -> Purple.copy(alpha = 0.28f)
        isPlaying    -> Purple.copy(alpha = 0.18f)
        focused      -> SurfaceVariant
        else         -> Surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 序号 / 正在播放指示
        Text(
            text     = if (isPlaying) "▶" else "${position + 1}",
            fontSize = 14.sp,
            color    = if (isPlaying) Purple else OnSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )

        AsyncImage(
            model              = song.picUrl(200),
            contentDescription = song.name,
            modifier           = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceVariant),
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text       = song.name,
                fontSize   = 17.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color      = if (isPlaying || isReordering) Purple else MaterialTheme.colorScheme.onSurface,
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

        // 拖拽排序切换（≡ 把手）
        Text(
            text     = "≡",
            fontSize = 20.sp,
            color    = if (isReordering) Purple else OnSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onToggleReorder)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )

        // 上移 / 下移（仅 reorder 模式时高亮）
        val arrowColor = if (isReordering) Purple else OnSurfaceVariant.copy(alpha = 0.5f)
        if (position > 0) {
            Text(
                text     = "▲",
                fontSize = 16.sp,
                color    = arrowColor,
                modifier = Modifier
                    .clickable(onClick = onMoveUp)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        } else {
            Spacer(Modifier.width(28.dp))
        }
        if (position < total - 1) {
            Text(
                text     = "▼",
                fontSize = 16.sp,
                color    = arrowColor,
                modifier = Modifier
                    .clickable(onClick = onMoveDown)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        } else {
            Spacer(Modifier.width(28.dp))
        }
        Text(
            text     = "✕",
            fontSize = 18.sp,
            color    = OnSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(8.dp),
        )
    }
}

