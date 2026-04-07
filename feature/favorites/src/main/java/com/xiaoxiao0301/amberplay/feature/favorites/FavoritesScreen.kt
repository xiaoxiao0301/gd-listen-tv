package com.xiaoxiao0301.amberplay.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun FavoritesScreen(
    onSongSelected: (Song) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    var batchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 30.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text("我的收藏", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
                Text("共 ${favorites.size} 首心动旋律", color = OnSurfaceVariant, fontSize = 16.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionBtn("全部播放", primary = true) {
                    favorites.firstOrNull()?.let(onSongSelected)
                }
                ActionBtn(if (batchMode) "退出批量" else "批量操作") {
                    batchMode = !batchMode
                    if (!batchMode) selectedIds = emptySet()
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有收藏歌曲", color = OnSurfaceVariant, fontSize = 18.sp)
            }
            return
        }

        HeaderRow(batchMode)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(favorites, key = { _, it -> it.id }) { index, song ->
                FavoriteRow(
                    index = index + 1,
                    song = song,
                    batchMode = batchMode,
                    selected = song.id in selectedIds,
                    onClick = {
                        if (batchMode) {
                            selectedIds = if (song.id in selectedIds) selectedIds - song.id else selectedIds + song.id
                        } else {
                            onSongSelected(song)
                        }
                    },
                    onToggleFavorite = { viewModel.removeFavorite(song) },
                )
            }
        }

        if (batchMode) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionBtn("全选") {
                    selectedIds = favorites.map { it.id }.toSet()
                }
                ActionBtn("移除 ${selectedIds.size} 首", primary = true) {
                    if (selectedIds.isNotEmpty()) {
                        viewModel.batchRemoveFavorites(selectedIds)
                        selectedIds = emptySet()
                        batchMode = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionBtn(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Text(
        text = if (primary) "▶ $text" else "☑ $text",
        color = if (primary) MaterialTheme.colorScheme.onPrimary else OnSurface,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (primary) Amber else SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
    )
}

@Composable
private fun HeaderRow(batchMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        if (batchMode) Spacer(Modifier.width(34.dp))
        Text("#", color = OnSurfaceVariant, modifier = Modifier.width(56.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("歌曲名", color = OnSurfaceVariant, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("时长", color = OnSurfaceVariant, modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("操作", color = OnSurfaceVariant, modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun FavoriteRow(
    index: Int,
    song: Song,
    batchMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val highlighted = selected || index == 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (highlighted) 1.dp else 0.dp,
                color = if (highlighted) OnSurfaceVariant.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .background(if (highlighted) Surface else SurfaceContainerLow.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (index == 1) Amber else Color.Transparent),
        )
        Spacer(Modifier.width(10.dp))

        if (batchMode) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Amber else SurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Text(index.toString().padStart(2, '0'), color = OnSurfaceVariant, modifier = Modifier.width(56.dp), fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = song.picUrl(),
                contentDescription = song.name,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface),
            )
            Column {
                Text(
                    song.name,
                    color = if (index == 1) Amber else OnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(song.artistText, color = OnSurfaceVariant, fontSize = 12.sp)
            }
        }

        Text(
            formatDuration(song.durationMs),
            color = OnSurfaceVariant,
            modifier = Modifier.width(110.dp),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.width(140.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            ActionIcon("▶", Amber, onClick)
            Spacer(Modifier.width(8.dp))
            ActionIcon("♥", MaterialTheme.colorScheme.error, onToggleFavorite)
        }
    }
}

@Composable
private fun ActionIcon(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 10.dp),
    )
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}
