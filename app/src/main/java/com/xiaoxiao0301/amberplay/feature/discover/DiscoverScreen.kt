package com.xiaoxiao0301.amberplay.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.feature.home.HomeViewModel
import com.xiaoxiao0301.amberplay.feature.search.SearchUiState
import com.xiaoxiao0301.amberplay.feature.search.SearchViewModel

private val QUICK_CHIPS = listOf("周杰伦", "爵士乐", "落日飞车", "城市民谣", "复古合成器")

@Composable
fun DiscoverScreen(
    initialKeyword: String = "",
    onSongSelected: (Song) -> Unit,
    onAlbumClick: (source: String, albumId: String) -> Unit,
    onArtistClick: (source: String, artistName: String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val recentSongs by homeViewModel.recentSongs.collectAsStateWithLifecycle()
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by searchViewModel.favoriteIds.collectAsStateWithLifecycle()

    var keyword by remember { mutableStateOf(initialKeyword) }
    var resultTab by remember { mutableStateOf(ResultTab.Songs) }

    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotBlank()) {
            keyword = initialKeyword
            searchViewModel.search(initialKeyword)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.72f)) {
            SearchInput(
                query = keyword,
                onQueryChange = { keyword = it },
                onSearch = {
                    val q = keyword.trim()
                    if (q.isBlank()) return@SearchInput
                    searchViewModel.search(q)
                },
            )

            Spacer(Modifier.height(14.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(QUICK_CHIPS) { chip ->
                    Text(
                        text = chip,
                        color = OnSurface,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerHigh)
                            .clickable {
                                keyword = chip
                                searchViewModel.search(chip)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        val showResults = keyword.isNotBlank() && uiState !is SearchUiState.Idle
        if (showResults) {
            SearchResultHeader(
                keyword = keyword,
                selected = resultTab,
                onTabChange = { resultTab = it },
            )
            Spacer(Modifier.height(14.dp))
            DiscoverResultContent(
                uiState = uiState,
                favoriteIds = favoriteIds,
                mode = resultTab,
                onSongSelected = onSongSelected,
                onFavorite = { song -> searchViewModel.toggleFavorite(song) },
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onLoadMore = { searchViewModel.loadNextPage() },
            )
        } else {
            Text("最近播放", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Spacer(Modifier.height(14.dp))

            if (recentSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无播放记录", color = OnSurfaceVariant, fontSize = 18.sp)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(end = 24.dp),
                ) {
                    items(recentSongs.take(8)) { song ->
                        RecentSongCard(song = song, onClick = { onSongSelected(song) })
                    }
                }
            }
        }
    }
}

private enum class ResultTab { Songs, Artists }

@Composable
private fun SearchResultHeader(
    keyword: String,
    selected: ResultTab,
    onTabChange: (ResultTab) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Search Results", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(OnSurfaceVariant.copy(alpha = 0.16f)),
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = keyword,
        color = OnSurface,
        fontSize = 48.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OnSurfaceVariant.copy(alpha = 0.10f)),
    ) {
        Spacer(Modifier.height(1.dp))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
        ResultTabText(label = "歌曲", active = selected == ResultTab.Songs) { onTabChange(ResultTab.Songs) }
        ResultTabText(label = "歌手", active = selected == ResultTab.Artists) { onTabChange(ResultTab.Artists) }
    }
}

@Composable
private fun ResultTabText(label: String, active: Boolean, onClick: () -> Unit) {
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
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⌕", fontSize = 20.sp, color = OnSurfaceVariant)
        Spacer(Modifier.width(14.dp))
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
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Amber)
                .clickable(onClick = onSearch)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun DiscoverResultContent(
    uiState: SearchUiState,
    favoriteIds: Set<String>,
    mode: ResultTab,
    onSongSelected: (Song) -> Unit,
    onFavorite: (Song) -> Unit,
    onAlbumClick: (source: String, albumId: String) -> Unit,
    onArtistClick: (source: String, artistName: String) -> Unit,
    onLoadMore: () -> Unit,
) {
    when (uiState) {
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
                Text("搜索失败：${uiState.message}", color = MaterialTheme.colorScheme.error)
            }
        }

        is SearchUiState.Results -> {
            if (uiState.songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到相关结果", color = OnSurfaceVariant)
                }
                return
            }

            if (mode == ResultTab.Artists) {
                val grouped = uiState.songs.groupBy { it.artists.firstOrNull() ?: it.artistText }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(grouped.keys.toList()) { index, artist ->
                        val songs = grouped[artist].orEmpty()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (index % 2 == 0) SurfaceContainerLow else SurfaceContainerHigh)
                                .clickable { onArtistClick(songs.firstOrNull()?.source ?: "", artist) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerHigh),
                            ) {
                                AsyncImage(
                                    model = songs.firstOrNull()?.picUrl(),
                                    contentDescription = artist,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(artist, color = OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${songs.size} 首歌曲", color = OnSurfaceVariant, fontSize = 13.sp)
                            }
                            Text(
                                text = "→",
                                color = Amber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Amber.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                return
            }

            val topSong = uiState.songs.first()
            var topArtistFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(modifier = Modifier.weight(0.36f)) {
                    Text("最佳匹配歌手", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = if (topArtistFocused) 1.dp else 0.dp,
                                color = if (topArtistFocused) Amber.copy(alpha = 0.35f) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                            )
                            .background(SurfaceContainerHigh)
                            .onFocusChanged { topArtistFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                onArtistClick(topSong.source, topSong.artists.firstOrNull() ?: topSong.artistText)
                            },
                    ) {
                        AsyncImage(
                            model = topSong.picUrl(),
                            contentDescription = topSong.artistText,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0x22000000), Color(0x7A000000)),
                                    ),
                                ),
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp),
                        ) {
                            Text(topSong.artistText, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                            Text("${topSong.source.uppercase()} • 热门歌手", color = Color(0xFFE8D9C4), fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "▶ 播放全部",
                                color = OnSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (topArtistFocused) Color.White else AmberContainer)
                                    .clickable { onSongSelected(topSong) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(0.64f)) {
                    Text("单曲结果", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.songs.take(20)) { song ->
                            SongRow(
                                song = song,
                                favorite = song.id in favoriteIds,
                                onClick = { onSongSelected(song) },
                                onFavorite = { onFavorite(song) },
                                onAlbumClick = { onAlbumClick(song.source, song.album) },
                            )
                        }
                        if (uiState.hasMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "查看更多搜索结果",
                                        color = Amber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(AmberContainer)
                                            .clickable(onClick = onLoadMore)
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SearchUiState.Idle -> Unit
    }
}

@Composable
private fun SongRow(
    song: Song,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAlbumClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val duration = formatDuration(song.durationMs)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (focused) 1.dp else 0.dp,
                color = if (focused) Amber.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .background(
                when {
                    focused -> AmberContainer.copy(alpha = 0.56f)
                    favorite -> SurfaceContainerHigh
                    else -> SurfaceContainerLow
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AsyncImage(
                model = song.picUrl(),
                contentDescription = song.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariant),
            )
            if (focused) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artistText} • ${song.album}", fontSize = 13.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(duration, color = OnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionCircle(
                icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                color = if (favorite) Amber else OnSurfaceVariant,
                emphasized = favorite,
                onClick = onFavorite,
            )
            ActionCircle(
                icon = Icons.Filled.MoreHoriz,
                color = if (focused) OnSurface else OnSurfaceVariant,
                emphasized = focused,
                onClick = onAlbumClick,
            )
        }
    }
}

@Composable
private fun ActionCircle(
    icon: ImageVector,
    color: Color,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (emphasized) AmberContainer else SurfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun RecentSongCard(song: Song, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(256.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (focused) AmberContainer else SurfaceContainerLow)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(16.dp),
    ) {
        Box {
            AsyncImage(
                model = song.picUrl(),
                contentDescription = song.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
            )
            if (focused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = song.name,
            color = OnSurface,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artistText,
            color = OnSurfaceVariant,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
