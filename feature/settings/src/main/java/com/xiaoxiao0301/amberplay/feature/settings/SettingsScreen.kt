package com.xiaoxiao0301.amberplay.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode

private val BITRATE_OPTIONS = listOf(128, 192, 320, 740, 999)
private val BITRATE_LABELS  = listOf("128 kbps", "192 kbps", "320 kbps", "740 kbps（无损）", "999 kbps（无损）")
private val SPEED_OPTIONS   = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val SPEED_LABELS    = listOf("0.5x", "0.75x", "1.0x（默认）", "1.25x", "1.5x", "2.0x")
private val SLEEP_OPTIONS      = listOf(0, 15, 30, 45, 60)
private val SLEEP_LABELS       = listOf("关闭", "15 分钟", "30 分钟", "45 分钟", "60 分钟")
private val CROSSFADE_OPTIONS  = listOf(0, 500, 1000, 2000)
private val CROSSFADE_LABELS   = listOf("关闭", "500 ms", "1000 ms", "2000 ms")
private val SOURCE_OPTIONS  = listOf("netease", "kuwo", "joox", "bilibili")
private val SOURCE_LABELS   = listOf("网易云音乐", "酷我音乐", "JOOX", "哔哩哔哩")
private val CACHE_OPTIONS   = listOf(256, 512, 1024, 2048, 4096)
private val CACHE_LABELS    = listOf("256 MB", "512 MB", "1 GB（默认）", "2 GB", "4 GB")

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings         by viewModel.settings.collectAsStateWithLifecycle()
    val cacheUsedMb      by viewModel.cacheUsedMb.collectAsStateWithLifecycle()
    val remainingTokens  by viewModel.remainingTokens.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title            = { Text("确认清除缓存") },
            text             = { Text("将清空所有已缓存的音频文件，是否继续？") },
            confirmButton    = {
                TextButton(onClick = { viewModel.clearCache(); showClearDialog = false }) {
                    Text("确定", color = Purple)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(24.dp))

        // ─── 播放设置 ─────────────────────────────────────────────
        SettingsSection("播放设置") {
            // 音质选择
            SettingsLabel("音质偏好")
            RadioGroup(
                options   = BITRATE_OPTIONS,
                labels    = BITRATE_LABELS,
                selected  = settings.preferredBitrate,
                onSelect  = { viewModel.setBitrate(it) },
            )
            Spacer(Modifier.height(12.dp))

            // 播放速度
            SettingsLabel("播放速度")
            RadioGroup(
                options  = SPEED_OPTIONS,
                labels   = SPEED_LABELS,
                selected = settings.playbackSpeed,
                onSelect = { viewModel.setPlaybackSpeed(it) },
            )
            Spacer(Modifier.height(12.dp))

            // 睡眠定时
            SettingsLabel("睡眠定时")
            RadioGroup(
                options  = SLEEP_OPTIONS,
                labels   = SLEEP_LABELS,
                selected = settings.sleepTimerMin,
                onSelect = { viewModel.setSleepTimerMin(it) },
            )

            // 淡入淡出
            SettingsLabel("淡入淡出")
            RadioGroup(
                options  = CROSSFADE_OPTIONS,
                labels   = CROSSFADE_LABELS,
                selected = settings.crossfadeMs,
                onSelect = { viewModel.setCrossfadeMs(it) },
            )
        }

        Spacer(Modifier.height(20.dp))

        // ─── 音源设置 ─────────────────────────────────────────────
        SettingsSection("音源设置") {
            SettingsLabel("默认音源")
            RadioGroup(
                options  = SOURCE_OPTIONS,
                labels   = SOURCE_LABELS,
                selected = settings.defaultSource,
                onSelect = { viewModel.setDefaultSource(it) },
            )
            Spacer(Modifier.height(12.dp))

            SwitchRow(
                label   = "多源并行搜索",
                checked = settings.multiSourceSearch,
                onToggle = { viewModel.setMultiSourceSearch(it) },
            )
            Spacer(Modifier.height(8.dp))

            SettingsLabel("启用的音源（多选）")
            SOURCE_OPTIONS.forEachIndexed { idx, source ->
                CheckboxRow(
                    label   = SOURCE_LABELS[idx],
                    checked = source in settings.enabledSources,
                    onToggle = { viewModel.toggleEnabledSource(source) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── 歌词设置 ─────────────────────────────────────────────
        SettingsSection("歌词设置") {
            SettingsLabel("歌词显示模式")
            RadioGroup(
                options  = LyricMode.entries,
                labels   = listOf("仅原文", "仅译文", "双语对照"),
                selected = settings.lyricMode,
                onSelect = { viewModel.setLyricMode(it) },
            )
        }

        Spacer(Modifier.height(20.dp))

        // ─── 缓存设置 ─────────────────────────────────────────────
        SettingsSection("缓存设置") {
            SettingsLabel("音频缓存上限")
            RadioGroup(
                options  = CACHE_OPTIONS,
                labels   = CACHE_LABELS,
                selected = settings.cacheLimitMb,
                onSelect = { viewModel.setCacheLimitMb(it) },
            )
            Spacer(Modifier.height(12.dp))

            // 缓存用量
            val limitMb = settings.cacheLimitMb.toLong().coerceAtLeast(1L)
            val progress = (cacheUsedMb.toFloat() / limitMb).coerceIn(0f, 1f)
            Text(
                text  = "已用 $cacheUsedMb MB / $limitMb MB",
                fontSize = 13.sp,
                color = OnSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress  = { progress },
                modifier  = Modifier.fillMaxWidth(),
                color     = Purple,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text     = "🗑 清除全部缓存",
                fontSize = 14.sp,
                color    = OnSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariant)
                    .clickable { showClearDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(12.dp))

            SwitchRow(
                label    = "离线模式（仅播放缓存歌曲）",
                checked  = settings.offlineMode,
                onToggle = { viewModel.setOfflineMode(it) },
            )
        }

        Spacer(Modifier.height(40.dp))

        // ─── 网络设置 ─────────────────────────────────────────────
        SettingsSection("网络") {
            Text(
                text     = "当前请求配额：$remainingTokens / 50",
                fontSize = 14.sp,
                color    = OnSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            val quotaProgress = (remainingTokens.toFloat() / 50f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { quotaProgress },
                modifier = Modifier.fillMaxWidth(),
                color    = Purple,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text     = "令牌桶容量 50，每 5 分钟充满，每 2 秒自动刷新",
                fontSize = 11.sp,
                color    = OnSurfaceVariant.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(16.dp),
    ) {
        Text(
            text       = title,
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Purple,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SurfaceVariant)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, fontSize = 15.sp, color = OnSurfaceVariant)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun <T> RadioGroup(
    options: List<T>,
    labels: List<String>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    options.forEachIndexed { index, option ->
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelect(option) }
                .focusable()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected  = selected == option,
                onClick   = { onSelect(option) },
                modifier  = Modifier.size(20.dp),
                colors    = RadioButtonDefaults.colors(selectedColor = Purple),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text     = labels.getOrNull(index) ?: option.toString(),
                fontSize = 16.sp,
                color    = if (selected == option) MaterialTheme.colorScheme.onSurface
                           else OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(!checked) }
            .focusable()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(checkedThumbColor = Purple,
                checkedTrackColor = Purple.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .focusable()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = { onToggle() },
            modifier        = Modifier.size(20.dp),
            colors          = CheckboxDefaults.colors(checkedColor = Purple),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

