package com.xiaoxiao0301.amberplay.feature.search

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.ui.TvFocusCard
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun AlbumDetailScreen(
    source: String,
    albumId: String,
    albumName: String = "专辑",
    onSongSelected: (Song) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(source, albumId) { viewModel.init(source, albumId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvFocusCard(
                onClick  = onBack,
                modifier = Modifier.padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    text     = "◀",
                    fontSize = 20.sp,
                    color    = Purple,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                text       = albumName,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(20.dp))

        when (val state = uiState) {
            is AlbumUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
            }
            is AlbumUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("错误：${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is AlbumUiState.Ready -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(state.songs) { index, song ->
                        AlbumTrackRow(
                            trackNumber = index + 1,
                            song        = song,
                            onClick     = { onSongSelected(song) },
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun AlbumTrackRow(
    trackNumber: Int,
    song: Song,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val picUrl = song.picUrl(200)

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
        // 序号
        Text(
            text     = "$trackNumber",
            fontSize = 16.sp,
            color    = OnSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )
        // 封面
        AsyncImage(
            model              = picUrl,
            contentDescription = song.name,
            modifier           = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        // 歌名 + 歌手
        Column(Modifier.weight(1f)) {
            Text(
                text       = song.name,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = song.artistText,
                fontSize = 13.sp,
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
                .background(androidx.compose.ui.graphics.Color(0x337C5CBF))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
