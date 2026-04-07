package com.xiaoxiao0301.amberplay.feature.stats

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerHigh
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerLow
import com.xiaoxiao0301.amberplay.core.common.ui.picUrl
import com.xiaoxiao0301.amberplay.domain.model.PlayStat

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val topSongs by viewModel.topSongs.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalPlayCount.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalPlayDurationMs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("本周") }

    val mostLovedArtist = topSongs
        .groupBy { it.song.artistText }
        .maxByOrNull { it.value.sumOf { stat -> stat.playCount } }
        ?.key ?: "暂无"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            HeroStatCard(
                modifier = Modifier.weight(0.66f),
                totalHours = (totalDurationMs / 3_600_000L).coerceAtLeast(0),
            )
            ArtistStatCard(
                modifier = Modifier.weight(0.34f),
                artist = mostLovedArtist,
                plays = topSongs.sumOf { it.playCount },
                topSong = topSongs.firstOrNull(),
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "最近单曲循环",
                value = topSongs.firstOrNull()?.song?.name ?: "暂无",
                icon = Icons.Filled.RepeatOne,
                iconBg = Color(0xFFE9F1FF),
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "播放总次数",
                value = "$totalCount 次",
                icon = Icons.Filled.BarChart,
                iconBg = Color(0xFFFFEEE1),
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "活跃时间段",
                value = "22:00 - 01:00",
                icon = Icons.Filled.Schedule,
                iconBg = AmberContainer,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放量排行榜", color = OnSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("今日", "本周", "年度").forEach { tab ->
                    val active = activeTab == tab
                    Text(
                        text = tab,
                        color = if (active) Surface else OnSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) Amber else SurfaceContainerHigh)
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceContainerLow),
        ) {
            RankHeaderRow()
            if (topSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有播放记录", color = OnSurfaceVariant, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(topSongs, key = { _, stat -> stat.song.id }) { index, stat ->
                        RankRow(rank = index + 1, stat = stat)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatCard(modifier: Modifier, totalHours: Long) {
    Box(
        modifier = modifier
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(AmberContainer)
            .padding(28.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BadgeMark(icon = Icons.Filled.Schedule)
                Text("听歌总时长", color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(totalHours.toString(), color = OnSurface, fontSize = 82.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text("小时", color = OnSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "在过去的一段时间里，你把大量时光留给了音乐。",
                color = OnSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = "♪",
            color = Amber.copy(alpha = 0.16f),
            fontSize = 160.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ArtistStatCard(modifier: Modifier, artist: String, plays: Int, topSong: PlayStat?) {
    Column(
        modifier = modifier
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceContainerHigh)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BadgeMark(icon = Icons.Filled.Person)
            Text("最爱歌手", color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(18.dp))

        Box {
            AsyncImage(
                model = topSong?.song?.picUrl() ?: "",
                contentDescription = artist,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Surface),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Amber),
                contentAlignment = Alignment.Center,
            ) {
                Text("1", color = Surface, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(artist, color = OnSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("累计播放 $plays 次", color = OnSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainerLow)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OnSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = OnSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Text(value, color = OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun BadgeMark(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun RankHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text("#", color = OnSurfaceVariant, modifier = Modifier.width(44.dp))
        Text("歌曲", color = OnSurfaceVariant, modifier = Modifier.weight(1f))
        Text("播放量", color = OnSurfaceVariant, modifier = Modifier.width(110.dp), maxLines = 1)
        Text("时长", color = OnSurfaceVariant, modifier = Modifier.width(80.dp), maxLines = 1)
    }
}

@Composable
private fun RankRow(rank: Int, stat: PlayStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString().padStart(2, '0'),
            color = if (rank == 1) Amber else OnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(44.dp),
        )

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = stat.song.picUrl(),
                contentDescription = stat.song.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerHigh),
            )
            Column {
                Text(stat.song.name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stat.song.artistText, color = OnSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Text("${stat.playCount}", color = OnSurface, modifier = Modifier.width(110.dp), fontWeight = FontWeight.SemiBold)
        Text(formatDuration(stat.totalMs), color = OnSurfaceVariant, modifier = Modifier.width(80.dp))
    }
}

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
