package com.xiaoxiao0301.amberplay.feature.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun QueueScreen(
    onSongSelected: () -> Unit = {},
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentSongId = playback.currentSong?.id

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceContainerHigh.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface.copy(alpha = 0.35f)),
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
                .background(Surface)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("播放队列", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                        .border(1.dp, OnSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                        .clickable { viewModel.clear() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        tint = OnSurface,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(text = "清空", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.size(14.dp))

            if (queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("播放队列为空", color = OnSurfaceVariant, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                        QueueRow(
                            song = song,
                            active = song.id == currentSongId,
                            onClick = {
                                viewModel.playAt(index)
                                onSongSelected()
                            },
                            onDelete = { viewModel.remove(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    song: Song,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val duration = formatDuration(song.durationMs)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .scale(if (active) 1.02f else 1f)
            .shadow(if (active) 12.dp else 0.dp, RoundedCornerShape(14.dp))
            .background(if (active) AmberContainer else SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerHigh),
        ) {
            AsyncImage(
                model = song.picUrl(),
                contentDescription = song.name,
                modifier = Modifier.fillMaxSize(),
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Amber.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = if (active) Amber else OnSurface,
                fontSize = 18.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistText,
                color = OnSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(duration, color = OnSurfaceVariant, fontSize = 12.sp)
        if (active) {
            Spacer(Modifier.width(4.dp))
            Text("NOW", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))

        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "删除",
            tint = if (active) Amber else OnSurfaceVariant,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Color.White.copy(alpha = 0.5f) else Color.Transparent)
                .clickable(onClick = onDelete)
                .padding(6.dp),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}
