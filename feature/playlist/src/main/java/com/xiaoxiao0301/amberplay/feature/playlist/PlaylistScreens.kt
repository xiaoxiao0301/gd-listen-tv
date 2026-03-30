package com.xiaoxiao0301.amberplay.feature.playlist

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    var newPlaylistName by remember { mutableStateOf("") }

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
            Text(
                text     = "+ 新建",
                fontSize = 16.sp,
                color    = Purple,
                modifier = Modifier
                    .clickable { viewModel.openCreateDialog() }
                    .padding(8.dp),
            )
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
            .focusable()
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
        Text("🗑", fontSize = 20.sp, modifier = Modifier.clickable(onClick = onDelete).padding(8.dp))
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    onSongSelected: (Song) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(playlistId) { viewModel.init(playlistId) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        Text("歌单", fontSize = 26.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("歌单为空\n从搜索结果收藏歌曲后可在此处播放",
                    color = OnSurfaceVariant, fontSize = 18.sp,
                    textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(songs) { index, song ->
                    PlaylistSongRow(
                        index    = index,
                        song     = song,
                        onClick  = { viewModel.playSong(song); onSongSelected(song) },
                        onRemove = { viewModel.removeSong(song.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) SurfaceVariant else Surface)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}", fontSize = 14.sp, color = OnSurfaceVariant,
            modifier = Modifier.width(32.dp))
        val picUrl = "https://music-api.gdstudio.xyz/api.php" +
                "?types=pic&source=${song.source}&id=${song.picId}&size=200"
        AsyncImage(model = picUrl, contentDescription = song.name,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceVariant))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(song.artistText, fontSize = 13.sp, color = OnSurfaceVariant, maxLines = 1)
        }
        Text("✕", fontSize = 18.sp, color = OnSurfaceVariant,
            modifier = Modifier.clickable(onClick = onRemove).padding(8.dp))
    }
}
