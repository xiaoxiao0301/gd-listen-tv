package com.xiaoxiao0301.amberplay.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.xiaoxiao0301.amberplay.domain.model.PlayStat

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val topSongs       by viewModel.topSongs.collectAsStateWithLifecycle()
    val totalCount     by viewModel.totalPlayCount.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalPlayDurationMs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("播放统计", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(24.dp))

        // ─── 总览卡片 ─────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value    = totalCount.toString(),
                label    = "累计播放次数",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value    = formatDuration(totalDurationMs),
                label    = "累计播放时长",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value    = topSongs.size.toString(),
                label    = "有记录的歌曲",
            )
        }

        Spacer(Modifier.height(28.dp))

        if (topSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有播放记录", color = OnSurfaceVariant, fontSize = 16.sp)
            }
        } else {
            // ─── 播放次数柱状图 ────────────────────────────────────
            Text("播放次数 Top ${topSongs.size}", fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            PlayCountBarChart(topSongs)

            Spacer(Modifier.height(28.dp))

            // ─── 排行榜列表 ────────────────────────────────────────
            Text("详细排行榜", fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            topSongs.forEachIndexed { index, stat ->
                StatSongRow(rank = index + 1, stat = stat)
                if (index < topSongs.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Column(
        modifier         = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Purple)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = OnSurfaceVariant)
    }
}

@Composable
private fun PlayCountBarChart(stats: List<PlayStat>) {
    val maxCount = stats.maxOfOrNull { it.playCount }?.coerceAtLeast(1) ?: 1
    val barColor  = Purple
    val bgColor   = SurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(16.dp),
    ) {
        stats.forEach { stat ->
            val fraction = stat.playCount.toFloat() / maxCount.toFloat()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                // 歌名标签（固定宽度）
                Text(
                    text     = stat.song.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(140.dp),
                )
                Spacer(Modifier.width(8.dp))
                // 柱状条
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                ) {
                    // 背景
                    drawRoundRect(
                        color        = bgColor,
                        size         = Size(size.width, size.height),
                        cornerRadius = CornerRadius(6f),
                    )
                    // 前景
                    if (fraction > 0f) {
                        drawRoundRect(
                            color        = barColor,
                            size         = Size(size.width * fraction, size.height),
                            cornerRadius = CornerRadius(6f),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // 次数
                Text(
                    text     = "${stat.playCount}次",
                    fontSize = 13.sp,
                    color    = OnSurfaceVariant,
                    modifier = Modifier.width(46.dp),
                )
            }
        }
    }
}

@Composable
private fun StatSongRow(rank: Int, stat: PlayStat) {
    val picUrl = stat.song.picUrl(200)
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 排名
        Text(
            text       = "#$rank",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = if (rank <= 3) Purple else OnSurfaceVariant,
            modifier   = Modifier.width(36.dp),
        )
        // 封面
        AsyncImage(
            model              = picUrl,
            contentDescription = stat.song.name,
            modifier           = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        // 歌名 + 歌手
        Column(Modifier.weight(1f)) {
            Text(stat.song.name, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(stat.song.artistText, fontSize = 13.sp, color = OnSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // 播放次数 + 时长
        Column(horizontalAlignment = Alignment.End) {
            Text("${stat.playCount} 次", fontSize = 15.sp, color = Purple,
                fontWeight = FontWeight.SemiBold)
            Text(formatDuration(stat.totalMs), fontSize = 12.sp, color = OnSurfaceVariant)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours    = totalSec / 3600
    val minutes  = (totalSec % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

