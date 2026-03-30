package com.xiaoxiao0301.amberplay.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun SearchScreen(
    onSongSelected: (Song) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val history     by viewModel.searchHistory.collectAsStateWithLifecycle()
    val snackbar    = remember { SnackbarHostState() }
    var query       by remember { mutableStateOf("") }
    val fieldFocus  = remember { FocusRequester() }

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
            // ─── 搜索输入框 ──────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(fieldFocus),
                label         = { Text("搜索歌曲、歌手、专辑") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.search(query) }
                ),
            )

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
                    LazyColumn(
                        verticalArrangement  = Arrangement.spacedBy(8.dp),
                        contentPadding       = PaddingValues(vertical = 4.dp),
                    ) {
                        itemsIndexed(state.songs) { index, song ->
                            SongResultCard(
                                song    = song,
                                onClick = { onSongSelected(song) },
                            )
                            // 到达列表末尾时加载下一页
                            if (index == state.songs.lastIndex && state.hasMore) {
                                LaunchedEffect(state.page) { viewModel.loadNextPage() }
                            }
                        }
                    }
                }
                else -> { /* Idle — already shown history above */ }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SongResultCard(
    song: Song,
    onClick: () -> Unit,
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 专辑封面（Coil 异步加载）
        val picUrl = "https://music-api.gdstudio.xyz/api.php" +
                "?types=pic&source=${song.source}&id=${song.picId}&size=300"
        AsyncImage(
            model             = picUrl,
            contentDescription = song.name,
            modifier          = Modifier
                .size(64.dp)
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
            )
            Text(
                text     = "${song.artistText} · ${song.album}",
                fontSize = 14.sp,
                color    = OnSurfaceVariant,
                maxLines = 1,
            )
        }

        // 来源角标
        Text(
            text     = song.source.take(2).uppercase(),
            fontSize = 11.sp,
            color    = Purple,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x337C5CBF))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
