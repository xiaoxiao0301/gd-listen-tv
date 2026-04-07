package com.xiaoxiao0301.amberplay.feature.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun PlaylistListScreen(
    onPlaylistClick: (Int) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val showDialog by viewModel.showCreateDialog.collectAsStateWithLifecycle()
    val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()

    var newPlaylistName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Playlist?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportToUri(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importFromUri(uri) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeCreateDialog() },
            title = { Text("新建歌单") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("歌单名称") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                    }),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                }) {
                    Text("创建", color = Amber)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.closeCreateDialog()
                    newPlaylistName = ""
                }) {
                    Text("取消")
                }
            },
        )
    }

    pendingDelete?.let { target ->
        DeletePlaylistDialog(
            title = target.name,
            onDismiss = { pendingDelete = null },
            onDelete = {
                viewModel.deletePlaylist(target.id)
                pendingDelete = null
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 34.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("我的歌单", color = OnSurface, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp)
                    Text("你珍藏的 ${playlists.size} 个音乐合集", color = OnSurfaceVariant, fontSize = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderBtn("导出") {
                        val ts = System.currentTimeMillis()
                        exportLauncher.launch("pltv_backup_$ts.json")
                    }
                    HeaderBtn("导入") { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    HeaderBtn("新建", primary = true) { viewModel.openCreateDialog() }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有歌单，点击右上角新建", color = OnSurfaceVariant, fontSize = 18.sp)
                }
            } else {
                val featured = playlists.first()
                val others = playlists.drop(1)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        FeaturedPlaylistCard(
                            playlist = featured,
                            onClick = { onPlaylistClick(featured.id) },
                            onDelete = { pendingDelete = featured },
                        )
                    }

                    items(others, key = { it.id }) { playlist ->
                        GridPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            onDelete = { pendingDelete = playlist },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun HeaderBtn(label: String, primary: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (primary) Amber else SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (primary) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            color = if (primary) Color.White else OnSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FeaturedPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh),
            ) {
                AsyncImage(
                    model = "https://picsum.photos/960/560?playlist=${playlist.id}",
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x66000000)),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x44000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = "置顶",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x44FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = playlist.name,
                color = OnSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${playlist.songCount} 首歌曲", color = OnSurfaceVariant, fontSize = 14.sp)
        }

        Text(
            text = "DEL",
            color = MaterialTheme.colorScheme.error,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x22BA1A1A))
                .clickable(onClick = onDelete)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun GridPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerHigh),
        ) {
            AsyncImage(
                model = "https://picsum.photos/420/420?playlist=${playlist.id}",
                contentDescription = playlist.name,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x26000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${playlist.songCount} 首歌曲", color = OnSurfaceVariant, fontSize = 13.sp)
            }
            Text(
                text = "✕",
                color = OnSurfaceVariant,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .clip(RoundedCornerShape(999.dp))
                    .background(Surface)
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DeletePlaylistDialog(
    title: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除歌单") },
        text = { Text("确定删除《$title》吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    onSongSelected: (Song) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(playlistId) { viewModel.init(playlistId) }

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        viewModel.batchRemoveComplete.collect {
            selectedIds = emptySet()
            multiSelect = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (multiSelect && selectedIds.isNotEmpty()) 70.dp else 0.dp),
        ) {
            HeroHeader(
                title = playlist?.name ?: "歌单",
                songCount = songs.size,
                totalDurationMs = songs.sumOf { it.durationMs },
                onPlayAll = {
                    songs.firstOrNull()?.let(onSongSelected)
                },
                onShuffle = {
                    songs.shuffled().firstOrNull()?.let(onSongSelected)
                },
            )

            Column(modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("歌曲列表", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text(
                        text = if (multiSelect) "☒ 取消批量" else "☑ 批量移除",
                        color = if (multiSelect) OnSurface else OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (multiSelect) SurfaceVariant else SurfaceContainerLow)
                            .border(
                                width = 1.dp,
                                color = if (multiSelect) OnSurfaceVariant.copy(alpha = 0.28f) else OnSurfaceVariant.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp),
                            )
                            .clickable {
                                multiSelect = !multiSelect
                                selectedIds = emptySet()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                SongHeaderRow(multiSelect = multiSelect)
                Spacer(Modifier.height(8.dp))

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerLow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "歌单为空\n从搜索页添加歌曲后即可播放",
                            textAlign = TextAlign.Center,
                            color = OnSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(760.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(songs) { index, song ->
                            PlaylistSongRow(
                                index = index + 1,
                                song = song,
                                multiSelect = multiSelect,
                                isSelected = song.id in selectedIds,
                                onClick = {
                                    if (multiSelect) {
                                        selectedIds = if (song.id in selectedIds) {
                                            selectedIds - song.id
                                        } else {
                                            selectedIds + song.id
                                        }
                                    } else {
                                        onSongSelected(song)
                                    }
                                },
                                onRemove = { viewModel.removeSong(song.id) },
                            )
                        }
                    }
                }
            }
        }

        if (multiSelect && selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已选 ${selectedIds.size} 首", color = OnSurface, modifier = Modifier.weight(1f))
                Text(
                    text = "移除",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { viewModel.batchRemove(selectedIds) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(
    title: String,
    songCount: Int,
    totalDurationMs: Long,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .background(SurfaceContainerHigh),
    ) {
        AsyncImage(
            model = "https://picsum.photos/1800/780?hero=$title",
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceContainerHigh),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x22000000), Color(0x77000000)),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 48.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AsyncImage(
                model = "https://picsum.photos/420/420?cover=$title",
                contentDescription = title,
                modifier = Modifier
                    .size(256.dp)
                    .graphicsLayer { rotationZ = -3f }
                    .clip(RoundedCornerShape(16.dp)),
            )

            Column {
                Text(
                    text = "官方精选",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$songCount 首歌曲 • ${formatLongDuration(totalDurationMs)}",
                    color = Color(0xE6FFFFFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                Text(title, color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "让轻柔的吉他和温暖的人声陪伴你的午后时光。",
                    color = Color(0xE6FFFFFF),
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "播放全部",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Amber)
                            .clickable(onClick = onPlayAll)
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    )
                    Text(
                        text = "随机播放",
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                            .clickable(onClick = onShuffle)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SongHeaderRow(multiSelect: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiSelect) {
            Text("", modifier = Modifier.width(34.dp))
        }
        Text("#", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold)
        Text("标题", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1.6f), fontWeight = FontWeight.Bold)
        Text("专辑", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("时长", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    song: Song,
    multiSelect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Amber.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .background(if (isSelected) AmberContainer else SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiSelect) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Amber else SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            index.toString().padStart(2, '0'),
            color = OnSurfaceVariant,
            modifier = Modifier.width(36.dp),
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.weight(1.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = song.picUrl(),
                contentDescription = song.name,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerHigh),
            )
            Column {
                Text(song.name, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artistText, color = OnSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Text(song.album, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(
            formatDuration(song.durationMs),
            color = OnSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )

        if (!multiSelect) {
            Text(
                text = "✕",
                color = OnSurfaceVariant,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.42f))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private fun formatLongDuration(ms: Long): String {
    val totalMinutes = (ms / 60000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours 小时 $minutes 分钟" else "$minutes 分钟"
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
