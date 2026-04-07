package com.xiaoxiao0301.amberplay.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoxiao0301.amberplay.core.common.theme.Amber
import com.xiaoxiao0301.amberplay.core.common.theme.AmberContainer
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurface
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerHigh
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerLow
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode

private val BITRATE_OPTIONS = listOf(128, 192, 320, 740, 999)
private val BITRATE_LABELS = listOf("128", "192", "320", "740 无损", "999 无损")
private val SOURCE_OPTIONS = listOf("netease", "kuwo", "joox", "bilibili")
private val SOURCE_LABELS = listOf("网易云", "酷我", "JOOX", "哔哩哔哩")
private val CACHE_OPTIONS = listOf(256, 512, 1024, 2048, 4096)
private val CACHE_LABELS = listOf("256MB", "512MB", "1GB", "2GB", "4GB")

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheUsedMb by viewModel.cacheUsedMb.collectAsStateWithLifecycle()
    val remainingTokens by viewModel.remainingTokens.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清除缓存") },
            text = { Text("将清空所有已缓存文件，是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearDialog = false
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 30.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            Text("设置", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Text("当前请求配额 $remainingTokens / 50", color = OnSurfaceVariant, fontSize = 14.sp)
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            SettingCard(title = "音质设置", icon = Icons.Filled.GraphicEq, modifier = Modifier.weight(1f)) {
                OptionGrid(
                    labels = BITRATE_LABELS,
                    selectedIndex = BITRATE_OPTIONS.indexOf(settings.preferredBitrate),
                    onSelect = { idx -> viewModel.setBitrate(BITRATE_OPTIONS[idx]) },
                )
            }
            SettingCard(title = "音源设置", icon = Icons.Filled.SwapHoriz, modifier = Modifier.weight(1f)) {
                OptionGrid(
                    labels = SOURCE_LABELS,
                    selectedIndex = SOURCE_OPTIONS.indexOf(settings.defaultSource),
                    onSelect = { idx -> viewModel.setDefaultSource(SOURCE_OPTIONS[idx]) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            SettingCard(title = "歌词设置", icon = Icons.Filled.Lyrics, modifier = Modifier.weight(1f)) {
                val lyricLabels = listOf("原文", "译文", "双语")
                val modes = listOf(LyricMode.ORIGINAL, LyricMode.TRANSLATION, LyricMode.BILINGUAL)
                OptionGrid(
                    labels = lyricLabels,
                    selectedIndex = modes.indexOf(settings.lyricMode),
                    onSelect = { idx -> viewModel.setLyricMode(modes[idx]) },
                )
            }

            SettingCard(
                title = "离线模式",
                icon = Icons.Filled.CloudOff,
                modifier = Modifier.weight(1f),
                highlighted = true,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .clickable { viewModel.setOfflineMode(!settings.offlineMode) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("离线模式", color = OnSurface, fontWeight = FontWeight.Bold)
                        Text("仅播放缓存歌曲", color = OnSurfaceVariant, fontSize = 12.sp)
                    }
                    OfflineToggle(checked = settings.offlineMode)
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text("网络设置", color = OnSurface, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("每 5 分钟只允许 50 次请求", color = OnSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OptionGrid(
                    labels = listOf("关闭", "开启"),
                    selectedIndex = if (settings.multiSourceSearch) 1 else 0,
                    onSelect = { viewModel.setMultiSourceSearch(it == 1) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SettingCard(title = "缓存设置", icon = Icons.Filled.Storage, modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OptionGrid(
                        labels = CACHE_LABELS,
                        selectedIndex = CACHE_OPTIONS.indexOf(settings.cacheLimitMb),
                        onSelect = { idx -> viewModel.setCacheLimitMb(CACHE_OPTIONS[idx]) },
                    )

                    Spacer(Modifier.height(12.dp))
                    val limitMb = settings.cacheLimitMb.toLong().coerceAtLeast(1L)
                    val progress = (cacheUsedMb.toFloat() / limitMb).coerceIn(0f, 1f)
                    Text("当前占用 $cacheUsedMb MB / $limitMb MB", color = OnSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerHigh),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(14.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Surface)
                            .clickable { showClearDialog = true }
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = "清理缓存",
                            color = OnSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun SettingCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Amber.copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .background(if (highlighted) AmberContainer else SurfaceContainerLow)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                title,
                color = OnSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (focused) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Amber),
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun OptionGrid(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val rows = labels.chunked(3)
    var indexBase = 0
    rows.forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEachIndexed { localIdx, label ->
                val index = indexBase + localIdx
                val selected = index == selectedIndex
                Text(
                    text = label,
                    color = if (selected) Surface else OnSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Amber else Surface)
                        .border(
                            width = 2.dp,
                            color = if (selected) Amber else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        indexBase += row.size
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OfflineToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) Color(0x88FFFFFF) else SurfaceContainerHigh)
            .padding(horizontal = 4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (checked) Amber else OnSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}
