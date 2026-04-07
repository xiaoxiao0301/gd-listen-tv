package com.xiaoxiao0301.amberplay.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.domain.model.Song

private val QUICK_CHIPS = listOf("周杰伦", "爵士乐", "落日飞车", "城市民谣", "复古合成器")

@Composable
fun HomeScreen(
    onSongSelected: (Song) -> Unit = {},
    onSearchKeyword: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    var keyword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        SearchInput(
            query = keyword,
            onQueryChange = { keyword = it },
            onSearch = {
                val q = keyword.trim()
                if (q.isNotEmpty()) onSearchKeyword(q)
            },
        )

        Spacer(Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            QUICK_CHIPS.forEach { chip ->
                HistoryChip(text = chip) {
                    keyword = chip
                    onSearchKeyword(chip)
                }
            }
        }

        Spacer(Modifier.height(42.dp))

        Text(
            text = "最近播放",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface,
        )
        Spacer(Modifier.height(18.dp))

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

        Spacer(Modifier.height(28.dp))
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
            .background(SurfaceContainerHigh)
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
            color = Amber,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun HistoryChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = OnSurfaceVariant,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
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
                    Text("▶", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.source.take(2).uppercase(),
            color = Amber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33F5D7A1))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
