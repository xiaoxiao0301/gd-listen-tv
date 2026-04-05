package com.xiaoxiao0301.amberplay.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun SearchScreen(
    initialKeyword: String = "",
    onSongSelected: (Song) -> Unit = {},
    onAlbumClick: (source: String, albumId: String) -> Unit = { _, _ -> },
    onArtistClick: (source: String, artistName: String) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState          by viewModel.uiState.collectAsStateWithLifecycle()
    val history          by viewModel.searchHistory.collectAsStateWithLifecycle()
    val favoriteIds      by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists        by viewModel.playlists.collectAsStateWithLifecycle()
    val snackbar         = remember { SnackbarHostState() }
    var query            by remember { mutableStateOf(initialKeyword) }
    val fieldFocus       = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 从历史快速重搜：预填关键词并立即触发搜索
    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotBlank()) viewModel.search(initialKeyword)
    }

    // 加入歌单弹窗状态
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    // 多选模式
    var multiSelect  by remember { mutableStateOf(false) }
    var selectedIds  by remember { mutableStateOf(setOf<String>()) }
    // 批量"加入歌单"弹窗
    var batchPlaylistPending by remember { mutableStateOf(false) }

    // 歌手聚合分组模式
    var groupByArtist   by remember { mutableStateOf(false) }
    var expandedArtists by remember { mutableStateOf(setOf<String>()) }

    // 新搜索开始时折叠所有分组
    LaunchedEffect(uiState) {
        if (uiState is SearchUiState.Loading) expandedArtists = emptySet()
    }

    // 频率限制警告
    LaunchedEffect(Unit) {
        viewModel.rateLimitWarning.collect { waitSec ->
            snackbar.showSnackbar("请求过于频繁，请 ${waitSec}s 后重试")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            // ─── 搜索输入框 + 多选按钮 ───────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    modifier      = Modifier
                        .weight(1f)
                        .focusRequester(fieldFocus),
                    label         = { Text("搜索歌曲、歌手、专辑") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.search(query)
                        }
                    ),
                )
                Spacer(Modifier.width(12.dp))
                // 歌手分组切换（有结果时显示）
                if (uiState is SearchUiState.Results) {
                    Text(
                        text     = if (groupByArtist) "≡ 列表" else "👤 歌手",
                        color    = if (groupByArtist) Purple else OnSurfaceVariant,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariant)
                            .clickable {
                                groupByArtist = !groupByArtist
                                expandedArtists = emptySet()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text     = if (multiSelect) "✓ 退出" else "☑ 多选",
                    color    = if (multiSelect) Purple else OnSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant)
                        .clickable {
                            multiSelect = !multiSelect
                            if (!multiSelect) selectedIds = emptySet()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── 搜索历史 Chips（Idle 状态） ─────────────────────
            if (uiState is SearchUiState.Idle && history.isNotEmpty()) {
                Text(
                    "搜索历史",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = OnSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history) { keyword ->
                        SuggestionChip(
                            onClick = {
                                query = keyword
                                viewModel.search(keyword)
                            },
                            label = { Text(keyword) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ─── 状态显示 ────────────────────────────────────────
            when (val state = uiState) {
                is SearchUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Purple)
                    }
                }
                is SearchUiState.Empty -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("没有找到相关结果", color = OnSurfaceVariant)
                    }
                }
                is SearchUiState.Error -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("错误：${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is SearchUiState.Results -> {
                    if (groupByArtist) {
                        // ─── 歌手聚合视图 ─────────────────────────────────
                        val grouped = state.songs.groupBy { it.artists.firstOrNull() ?: it.artistText }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding      = PaddingValues(top = 4.dp, bottom = 80.dp),
                        ) {
                            grouped.forEach { (artist, artistSongs) ->
                                val isExpanded = artist in expandedArtists
                                item(key = "hdr_$artist") {
                                    ArtistGroupHeader(
                                        artistName   = artist,
                                        songCount    = artistSongs.size,
                                        isExpanded   = isExpanded,
                                        onToggle     = {
                                            expandedArtists = if (isExpanded)
                                                expandedArtists - artist
                                            else expandedArtists + artist
                                        },
                                        onOpenArtist = { onArtistClick(artistSongs.first().source, artist) },
                                    )
                                }
                                if (isExpanded) {
                                    items(artistSongs, key = { "song_${it.id}" }) { song ->
                                        SongResultCard(
                                            song            = song,
                                            isFavorite      = song.id in favoriteIds,
                                            isSelected      = song.id in selectedIds,
                                            multiSelect     = multiSelect,
                                            onClick = {
                                                if (multiSelect) {
                                                    selectedIds = if (song.id in selectedIds)
                                                        selectedIds - song.id else selectedIds + song.id
                                                } else {
                                                    onSongSelected(song)
                                                }
                                            },
                                            onFavorite      = { viewModel.toggleFavorite(song) },
                                            onPlayNext      = { viewModel.playNext(song) },
                                            onAddToPlaylist = { songForPlaylist = song },
                                        )
                                    }
                                }
                            }
                            if (state.hasMore) {
                                item { LaunchedEffect(state.page) { viewModel.loadNextPage() } }
                            }
                        }
                    } else {
                        // ─── 平铺列表视图 ─────────────────────────────────
                        LazyColumn(
                            verticalArrangement  = Arrangement.spacedBy(8.dp),
                            contentPadding       = PaddingValues(top = 4.dp, bottom = 80.dp),
                        ) {
                            itemsIndexed(state.songs) { index, song ->
                                SongResultCard(
                                    song            = song,
                                    isFavorite      = song.id in favoriteIds,
                                    isSelected      = song.id in selectedIds,
                                    multiSelect     = multiSelect,
                                    onClick = {
                                        if (multiSelect) {
                                            selectedIds = if (song.id in selectedIds)
                                                selectedIds - song.id else selectedIds + song.id
                                        } else {
                                            onSongSelected(song)
                                        }
                                    },
                                    onFavorite      = { viewModel.toggleFavorite(song) },
                                    onPlayNext      = { viewModel.playNext(song) },
                                    onAddToPlaylist = { songForPlaylist = song },
                                )
                                if (index == state.songs.lastIndex && state.hasMore) {
                                    LaunchedEffect(state.page) { viewModel.loadNextPage() }
                                }
                            }
                        }
                    }
                }
                else -> { /* Idle — already shown history above */ }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (multiSelect && selectedIds.isNotEmpty()) 72.dp else 0.dp))

        // ─── 批量操作底栏 ─────────────────────────────────────────
        if (multiSelect && selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(SurfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "已选 ${selectedIds.size} 首",
                    color    = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                val currentSongs = (uiState as? SearchUiState.Results)?.songs ?: emptyList()
                Text(
                    "❤ 批量收藏",
                    color    = Purple,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Purple.copy(alpha = 0.15f))
                        .clickable {
                            viewModel.addBatchToFavorites(currentSongs.filter { it.id in selectedIds })
                            selectedIds = emptySet()
                            multiSelect = false
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(
                    "➕ 加入歌单",
                    color    = Purple,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Purple.copy(alpha = 0.15f))
                        .clickable { batchPlaylistPending = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }

    // ─── 加入歌单弹窗（单曲） ─────────────────────────────────────
    val targetSong = songForPlaylist
    if (targetSong != null) {
        AddToPlaylistDialog(
            playlists   = playlists,
            onDismiss   = { songForPlaylist = null },
            onSelect    = { playlistId ->
                viewModel.addSongToPlaylist(targetSong, playlistId)
                songForPlaylist = null
            },
        )
    }

    // ─── 加入歌单弹窗（批量） ─────────────────────────────────────
    if (batchPlaylistPending) {
        val batchSongs = (uiState as? SearchUiState.Results)?.songs?.filter { it.id in selectedIds } ?: emptyList()
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { batchPlaylistPending = false },
            onSelect  = { playlistId ->
                viewModel.addBatchToPlaylist(batchSongs, playlistId)
                batchPlaylistPending = false
                selectedIds = emptySet()
                multiSelect = false
            },
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect:  (Int) -> Unit,
) {
    var selectedPlaylistId by remember(playlists) {
        mutableStateOf(playlists.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入歌单") },
        text  = {
            if (playlists.isEmpty()) {
                Text("还没有歌单，请先在歌单页面创建", color = OnSurfaceVariant)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick  = { selectedPlaylistId = playlist.id },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (selectedPlaylistId == playlist.id) "✓ ${playlist.name}" else playlist.name,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedPlaylistId != null,
                onClick = {
                    val id = selectedPlaylistId ?: return@TextButton
                    onSelect(id)
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SongResultCard(
    song: Song,
    isFavorite: Boolean,
    isSelected: Boolean,
    multiSelect: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val rowBg = when {
        isSelected -> Purple.copy(alpha = 0.22f)
        focused    -> SurfaceVariant
        else       -> Surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBg)
            .onFocusChanged { focused = it.hasFocus }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        // 多选复选标记 / 专辑封面
        if (multiSelect) {
            Box(
                modifier          = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Purple else SurfaceVariant),
                contentAlignment  = Alignment.Center,
            ) {
                if (isSelected) {
                    Text("✓", fontSize = 28.sp, color = Color.White)
                }
            }
        } else {
            val picUrl = song.picUrl()
            AsyncImage(
                model             = picUrl,
                contentDescription = song.name,
                modifier          = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant),
            )
        }

        Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
            Text(
                text       = song.name,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
            )
            Text(
                text     = "${song.artistText} · ${song.album}",
                fontSize = 14.sp,
                color    = OnSurfaceVariant,
                maxLines = 1,
            )
        }
        }

        // 收藏按钮
        Text(
            text     = if (isFavorite) "❤" else "🤍",
            fontSize = 20.sp,
            modifier = Modifier
                .clickable(onClick = onFavorite)
                .padding(8.dp),
        )

        if (!multiSelect) {
            // 下一首播放
            Text(
                text     = "⏭",
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable(onClick = onPlayNext)
                    .padding(8.dp),
            )

            // 加入歌单
            Text(
                text     = "➕",
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable(onClick = onAddToPlaylist)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ArtistGroupHeader(
    artistName: String,
    songCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenArtist: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Purple.copy(alpha = 0.15f) else SurfaceVariant)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = if (isExpanded) "▼" else "▶",
            fontSize = 14.sp,
            color    = Purple,
            modifier = Modifier.width(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text       = artistName,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = "$songCount 首",
                fontSize = 13.sp,
                color    = OnSurfaceVariant,
            )
        }
        Text(
            text     = "歌手页 ▶",
            fontSize = 13.sp,
            color    = Purple,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Purple.copy(alpha = 0.12f))
                .clickable(onClick = onOpenArtist)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
