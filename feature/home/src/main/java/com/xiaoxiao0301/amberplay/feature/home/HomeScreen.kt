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
import androidx.compose.ui.text.font.FontWeight
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
import com.xiaoxiao0301.amberplay.domain.model.Song

@Composable
fun HomeScreen(
    onSongSelected: (Song) -> Unit = {},
    onSearchKeyword: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recentSongs   by viewModel.recentSongs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text       = "主页",
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))

        // ─── 最近播放 ────────────────────────────────────────────
        if (recentSongs.isNotEmpty()) {
            SectionTitle("最近播放")
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding        = PaddingValues(end = 16.dp),
            ) {
                items(recentSongs) { song ->
                    RecentSongCard(song = song, onClick = { onSongSelected(song) })
                }
            }
            Spacer(Modifier.height(28.dp))
        } else {
            Box(
                modifier            = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment    = Alignment.Center,
            ) {
                Text("暂无播放记录，去搜索一些歌曲吧 🎵", color = OnSurfaceVariant, fontSize = 16.sp)
            }
            Spacer(Modifier.height(28.dp))
        }


    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text       = title,
        fontSize   = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun RecentSongCard(song: Song, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val picUrl = song.picUrl()

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) SurfaceVariant else Surface)
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model              = picUrl,
            contentDescription = song.name,
            modifier           = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text     = song.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text     = song.artistText,
            fontSize = 12.sp,
            color    = OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        // 来源角标
        Text(
            text     = song.source.take(2).uppercase(),
            fontSize = 10.sp,
            color    = Purple,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(androidx.compose.ui.graphics.Color(0x337C5CBF))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

