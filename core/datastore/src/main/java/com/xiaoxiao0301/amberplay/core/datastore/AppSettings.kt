package com.xiaoxiao0301.amberplay.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** 所有偏好设置的 Key 统一声明 */
object PreferencesKeys {
    val PREFERRED_BITRATE       = intPreferencesKey("preferred_bitrate")       // 128/192/320/740/999
    val DEFAULT_SOURCE          = stringPreferencesKey("default_source")        // "netease"
    val MULTI_SOURCE_SEARCH     = booleanPreferencesKey("multi_source_search")
    val ENABLED_SOURCES         = stringSetPreferencesKey("enabled_sources")
    val CACHE_LIMIT_MB          = intPreferencesKey("cache_limit_mb")           // MB
    val OFFLINE_MODE            = booleanPreferencesKey("offline_mode")
    val PLAYBACK_SPEED          = floatPreferencesKey("playback_speed")         // 0.5–2.0
    val CROSSFADE_MS            = intPreferencesKey("crossfade_ms")             // 0 = 关闭
    val SLEEP_TIMER_MIN         = intPreferencesKey("sleep_timer_min")          // 0 = 关闭
    val LYRIC_MODE              = stringPreferencesKey("lyric_mode")            // ORIGINAL/TRANSLATION/BILINGUAL
}

/** 歌词显示模式 */
enum class LyricMode { ORIGINAL, TRANSLATION, BILINGUAL }

/** 强类型应用设置快照（从 DataStore 读取后映射为此对象） */
data class AppSettings(
    val preferredBitrate: Int          = 999,
    val defaultSource: String          = "netease",
    val multiSourceSearch: Boolean     = false,
    val enabledSources: Set<String>    = setOf("netease", "kuwo", "joox", "bilibili"),
    val cacheLimitMb: Int              = 1024,
    val offlineMode: Boolean           = false,
    val playbackSpeed: Float           = 1.0f,
    val crossfadeMs: Int               = 0,
    val sleepTimerMin: Int             = 0,
    val lyricMode: LyricMode           = LyricMode.BILINGUAL,
)
