package com.xiaoxiao0301.amberplay.feature.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerHigh
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerLow
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var query by remember { mutableStateOf(initialKeyword) }
    var showSongTab by remember { mutableStateOf(true) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotBlank()) {
            viewModel.search(initialKeyword)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.rateLimitWarning.collect { waitSec ->
            snackbar.showSnackbar("请求过于频繁，请 ${waitSec}s 后重试")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
        ) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { if (query.isNotBlank()) viewModel.search(query.trim()) },
            )

            Spacer(Modifier.height(22.dp))

            val title = if (query.isBlank()) "搜索" else query
            Text(
                text = title,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SearchTab("歌曲", showSongTab) { showSongTab = true }
                SearchTab("歌手", !showSongTab) { showSongTab = false }
            }

            Spacer(Modifier.height(20.dp))

            when (val state = uiState) {
                SearchUiState.Idle -> {
                    if (history.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("输入关键词开始搜索", color = OnSurfaceVariant)
                        }
                    } else {
                        Column {
                            Text("搜索历史", color = OnSurfaceVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                history.take(6).forEach { keyword ->
                                    Text(
                                        text = keyword,
                                        color = OnSurfaceVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(SurfaceContainerHigh)
                                            .clickable {
                                                query = keyword
                                                viewModel.search(keyword)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                SearchUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber)
                    }
                }

                SearchUiState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("没有找到相关结果", color = OnSurfaceVariant)
                    }
                }

                is SearchUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("搜索失败：${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }

                is SearchUiState.Results -> {
                    if (showSongTab) {
                        SearchResultSongPane(
                            songs = state.songs,
                            favoriteIds = favoriteIds,
                            onSongSelected = onSongSelected,
                            onFavorite = { viewModel.toggleFavorite(it) },
                            onMore = { songForPlaylist = it },
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onLoadMore = {
                                if (state.hasMore) viewModel.loadNextPage()
                            },
                        )
                    } else {
                        ArtistResultPane(
                            songs = state.songs,
                            onArtistClick = onArtistClick,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    songForPlaylist?.let { target ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songForPlaylist = null },
            onSelect = { playlistId ->
                viewModel.addSongToPlaylist(target, playlistId)
                songForPlaylist = null
            },
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌕", color = OnSurfaceVariant, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isBlank()) {
                    Text("搜索歌曲、歌手", color = OnSurfaceVariant, fontSize = 18.sp)
                }
                inner()
            },
        )
        Text(
            text = "搜索",
            color = Amber,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SearchTab(label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = if (active) Amber else OnSurfaceVariant,
            fontSize = 22.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onClick),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) Amber else Color.Transparent),
        )
    }
}

@Composable
private fun SearchResultSongPane(
    songs: List<Song>,
    favoriteIds: Set<String>,
    onSongSelected: (Song) -> Unit,
    onFavorite: (Song) -> Unit,
    onMore: (Song) -> Unit,
    onAlbumClick: (source: String, albumId: String) -> Unit,
    onArtistClick: (source: String, artistName: String) -> Unit,
    onLoadMore: () -> Unit,
) {
    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无结果", color = OnSurfaceVariant)
        }
        return
    }

    val topSong = songs.first()
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(0.36f),
        ) {
            Text("最佳匹配歌手", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { onArtistClick(topSong.source, topSong.artists.firstOrNull() ?: topSong.artistText) },
            ) {
                AsyncImage(
                    model = topSong.picUrl(),
                    contentDescription = topSong.artistText,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000)),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                ) {
                    Text(topSong.artistText, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "播放全部",
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AmberContainer)
                            .clickable { onSongSelected(topSong) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(0.64f)) {
            Text("单曲结果", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(songs.take(20)) { song ->
                    SongCard(
                        song = song,
                        favorite = song.id in favoriteIds,
                        onClick = { onSongSelected(song) },
                        onFavorite = { onFavorite(song) },
                        onMore = { onMore(song) },
                        onAlbumClick = { onAlbumClick(song.source, song.album) },
                    )
                }
                item {
                    Text(
                        text = "查看更多搜索结果",
                        color = Amber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(22.dp))
                            .background(AmberContainer)
                            .clickable(onClick = onLoadMore)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    onAlbumClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.picUrl(),
            contentDescription = song.name,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artistText} • ${song.album}", fontSize = 13.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = if (favorite) "♥" else "♡",
            color = if (favorite) Amber else OnSurfaceVariant,
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onFavorite)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Text(
            text = "专辑",
            color = OnSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAlbumClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Text(
            text = "⋯",
            color = OnSurfaceVariant,
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onMore)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ArtistResultPane(
    songs: List<Song>,
    onArtistClick: (source: String, artistName: String) -> Unit,
) {
    val groups = songs.groupBy { it.artists.firstOrNull() ?: it.artistText }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { (artist, artistSongs) ->
            item(key = artist) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .clickable { onArtistClick(artistSongs.first().source, artist) }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("👤", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(artist, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${artistSongs.size} 首歌曲", color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                    Text("进入歌手页", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    var selectedPlaylistId by remember(playlists) {
        mutableStateOf(playlists.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入歌单") },
        text = {
            if (playlists.isEmpty()) {
                Text("还没有歌单，请先在歌单页面创建", color = OnSurfaceVariant)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { selectedPlaylistId = playlist.id },
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
                },
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
