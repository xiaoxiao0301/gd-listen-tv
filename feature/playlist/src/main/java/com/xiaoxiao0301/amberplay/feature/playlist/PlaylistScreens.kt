package com.xiaoxiao0301.amberplay.feature.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.Background
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun PlaylistListScreen(
    onPlaylistClick: (Int) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val playlists       by viewModel.playlists.collectAsStateWithLifecycle()
    val showDialog      by viewModel.showCreateDialog.collectAsStateWithLifecycle()
    val exportMessage   by viewModel.exportMessage.collectAsStateWithLifecycle()
    val snackbar        = remember { SnackbarHostState() }
    var newPlaylistName by remember { mutableStateOf("") }

    // Show export/import feedback in snackbar
    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    // SAF launcher for exporting
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportToUri(uri) }

    // SAF launcher for importing
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importFromUri(uri) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeCreateDialog() },
            title            = { Text("新建歌单") },
            text             = {
                OutlinedTextField(
                    value         = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label         = { Text("歌单名称") },
                    singleLine    = true,
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
                }) { Text("创建", color = Purple) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeCreateDialog(); newPlaylistName = "" }) {
                    Text("取消")
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
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
                text       = "我的歌单",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text     = "⬆ 导出",
                    fontSize = 15.sp,
                    color    = OnSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariant)
                        .clickable {
                            val ts = System.currentTimeMillis()
                            exportLauncher.launch("pltv_backup_$ts.json")
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(
                    text     = "⬇ 导入",
                    fontSize = 15.sp,
                    color    = OnSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariant)
                        .clickable { importLauncher.launch(arrayOf("application/json", "*/*")) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(
                    text     = "+ 新建",
                    fontSize = 15.sp,
                    color    = Purple,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Purple.copy(alpha = 0.12f))
                        .clickable { viewModel.openCreateDialog() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有歌单，点击右上角新建", color = OnSurfaceVariant, fontSize = 18.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(playlists) { playlist ->
                    PlaylistRow(
                        name      = playlist.name,
                        songCount = playlist.songCount,
                        onClick   = { onPlaylistClick(playlist.id) },
                        onDelete  = { viewModel.deletePlaylist(playlist.id) },
                    )
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbar,
        modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
    )
    } // end Box
}

@Composable
private fun PlaylistRow(
    name: String,
    songCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) SurfaceVariant else Surface)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📋", fontSize = 28.sp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text("$songCount 首歌曲", fontSize = 14.sp, color = OnSurfaceVariant)
        }
        Text(
            "🗑",
            fontSize = 20.sp,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .clickable(onClick = onDelete)
                .padding(8.dp),
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    onSongSelected: (Song) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(playlistId) { viewModel.init(playlistId) }
    val songs    by viewModel.songs.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    // ─── 多选状态 ────────────────────────────────────────────────
    var multiSelect  by remember { mutableStateOf(false) }
    var selectedIds  by remember { mutableStateOf(setOf<String>()) }

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
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        // ─── 标题栏 ──────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(playlist?.name ?: "歌单", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                if (songs.isNotEmpty()) {
                    Text("${songs.size} 首", fontSize = 14.sp, color = OnSurfaceVariant)
                }
            }
            if (songs.isNotEmpty()) {
                Text(
                    text     = if (multiSelect) "取消" else "批量",
                    fontSize = 15.sp,
                    color    = if (multiSelect) OnSurfaceVariant else Purple,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (multiSelect) SurfaceVariant else Purple.copy(alpha = 0.12f))
                        .clickable {
                            multiSelect = !multiSelect
                            selectedIds = emptySet()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("歌单为空\n从搜索结果收藏歌曲后可在此处播放",
                    color = OnSurfaceVariant, fontSize = 18.sp,
                    textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier             = Modifier.weight(1f),
                verticalArrangement  = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(songs) { index, song ->
                    PlaylistSongRow(
                        index      = index,
                        song       = song,
                        multiSelect = multiSelect,
                        isSelected  = song.id in selectedIds,
                        onClick = {
                            if (multiSelect) {
                                selectedIds = if (song.id in selectedIds)
                                    selectedIds - song.id else selectedIds + song.id
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

    // ─── 批量操作底栏 ────────────────────────────────────────────
    if (multiSelect && selectedIds.isNotEmpty()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SurfaceVariant)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text     = "已选 ${selectedIds.size} 首",
                fontSize = 15.sp,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text     = "移除",
                fontSize = 15.sp,
                color    = Color.Red,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
                    .clickable {
                        viewModel.batchRemove(selectedIds)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
    } // end Box
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    song: Song,
    multiSelect: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        isSelected -> Purple.copy(alpha = 0.22f)
        focused    -> SurfaceVariant
        else       -> Surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiSelect) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Purple else SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Text("✓", fontSize = 18.sp, color = Color.White)
            }
        } else {
            Text("${index + 1}", fontSize = 14.sp, color = OnSurfaceVariant,
                modifier = Modifier.width(32.dp))
            val picUrl = song.picUrl(200)
            AsyncImage(model = picUrl, contentDescription = song.name,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceVariant))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(song.artistText, fontSize = 13.sp, color = OnSurfaceVariant, maxLines = 1)
        }
        if (!multiSelect) {
            Text("✕", fontSize = 18.sp, color = OnSurfaceVariant,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .clickable(onClick = onRemove)
                    .padding(8.dp))
        }
    }
}
