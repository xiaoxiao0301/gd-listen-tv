package com.xiaoxiao0301.amberplay.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun FavoritesScreen(
    onSongSelected: (Song) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedIds     by remember { mutableStateOf(emptySet<String>()) }

    // Exit multi-select when list becomes empty
    if (favorites.isEmpty() && multiSelectMode) {
        multiSelectMode = false
        selectedIds     = emptySet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        // ─── Header ─────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "我的收藏",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (multiSelectMode) {
                    Text(
                        text     = "已选 ${selectedIds.size} / ${favorites.size}",
                        fontSize = 14.sp,
                        color    = OnSurfaceVariant,
                    )
                    Text(
                        text     = "取消",
                        fontSize = 14.sp,
                        color    = OnSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariant)
                            .clickable { multiSelectMode = false; selectedIds = emptySet() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                } else {
                    Text(
                        text     = "${favorites.size} 首",
                        fontSize = 16.sp,
                        color    = OnSurfaceVariant,
                    )
                    if (favorites.isNotEmpty()) {
                        Text(
                            text     = "☑ 多选",
                            fontSize = 14.sp,
                            color    = Purple,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Purple.copy(alpha = 0.12f))
                                .clickable { multiSelectMode = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有收藏歌曲\n在搜索结果中点击 ❤ 来添加", color = OnSurfaceVariant,
                    fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            // ─── List ────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(favorites) { _, song ->
                    FavoriteSongRow(
                        song            = song,
                        multiSelectMode = multiSelectMode,
                        isSelected      = song.id in selectedIds,
                        onClick         = {
                            if (multiSelectMode) {
                                selectedIds = if (song.id in selectedIds)
                                    selectedIds - song.id else selectedIds + song.id
                            } else {
                                onSongSelected(song)
                            }
                        },
                        onRemove        = { viewModel.removeFavorite(song) },
                    )
                }
            }

            // ─── Multi-select action bar ─────────────────────────────
            if (multiSelectMode) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text     = "移除收藏（${selectedIds.size}）",
                        fontSize = 15.sp,
                        color    = if (selectedIds.isEmpty()) OnSurfaceVariant
                                   else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedIds.isEmpty()) SurfaceVariant
                                        else Purple.copy(alpha = 0.18f))
                            .clickable(enabled = selectedIds.isNotEmpty()) {
                                viewModel.batchRemoveFavorites(selectedIds)
                                selectedIds     = emptySet()
                                multiSelectMode = false
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Text(
                        text     = "全选",
                        fontSize = 15.sp,
                        color    = OnSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariant)
                            .clickable { selectedIds = favorites.map { it.id }.toSet() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteSongRow(
    song: Song,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(when {
                isSelected -> Purple.copy(alpha = 0.18f)
                focused    -> SurfaceVariant
                else       -> Surface
            })
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiSelectMode) {
            Checkbox(
                checked         = isSelected,
                onCheckedChange = { onClick() },
                colors          = CheckboxDefaults.colors(checkedColor = Purple),
                modifier        = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
        }

        val picUrl = "https://music-api.gdstudio.xyz/api.php" +
                "?types=pic&source=${song.source}&id=${song.picId}&size=300"
        AsyncImage(
            model              = picUrl,
            contentDescription = song.name,
            modifier           = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant),
        )

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text       = song.name,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = "${song.artistText} · ${song.album}",
                fontSize = 14.sp,
                color    = OnSurfaceVariant,
                maxLines = 1,
            )
        }

        // 取消收藏（单选模式下显示）
        if (!multiSelectMode) {
            Text(
                text     = "❤",
                fontSize = 22.sp,
                color    = Purple,
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(8.dp),
            )
        }
    }
}
