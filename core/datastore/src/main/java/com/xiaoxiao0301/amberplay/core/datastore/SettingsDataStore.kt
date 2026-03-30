package com.xiaoxiao0301.amberplay.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            preferredBitrate  = prefs[PreferencesKeys.PREFERRED_BITRATE]   ?: 999,
            defaultSource     = prefs[PreferencesKeys.DEFAULT_SOURCE]       ?: "netease",
            multiSourceSearch = prefs[PreferencesKeys.MULTI_SOURCE_SEARCH]  ?: false,
            enabledSources    = prefs[PreferencesKeys.ENABLED_SOURCES]
                                ?: setOf("netease", "kuwo", "joox", "bilibili"),
            cacheLimitMb      = prefs[PreferencesKeys.CACHE_LIMIT_MB]       ?: 1024,
            offlineMode       = prefs[PreferencesKeys.OFFLINE_MODE]         ?: false,
            playbackSpeed     = prefs[PreferencesKeys.PLAYBACK_SPEED]       ?: 1.0f,
            crossfadeMs       = prefs[PreferencesKeys.CROSSFADE_MS]         ?: 0,
            sleepTimerMin     = prefs[PreferencesKeys.SLEEP_TIMER_MIN]      ?: 0,
            lyricMode         = LyricMode.entries.firstOrNull {
                it.name == prefs[PreferencesKeys.LYRIC_MODE]
            } ?: LyricMode.BILINGUAL,
        )
    }

    suspend fun setBitrate(br: Int) =
        dataStore.edit { it[PreferencesKeys.PREFERRED_BITRATE] = br }

    suspend fun setDefaultSource(source: String) =
        dataStore.edit { it[PreferencesKeys.DEFAULT_SOURCE] = source }

    suspend fun setMultiSourceSearch(enabled: Boolean) =
        dataStore.edit { it[PreferencesKeys.MULTI_SOURCE_SEARCH] = enabled }

    suspend fun setEnabledSources(sources: Set<String>) =
        dataStore.edit { it[PreferencesKeys.ENABLED_SOURCES] = sources }

    suspend fun setCacheLimitMb(mb: Int) =
        dataStore.edit { it[PreferencesKeys.CACHE_LIMIT_MB] = mb }

    suspend fun setOfflineMode(enabled: Boolean) =
        dataStore.edit { it[PreferencesKeys.OFFLINE_MODE] = enabled }

    suspend fun setPlaybackSpeed(speed: Float) =
        dataStore.edit { it[PreferencesKeys.PLAYBACK_SPEED] = speed }

    suspend fun setCrossfadeMs(ms: Int) =
        dataStore.edit { it[PreferencesKeys.CROSSFADE_MS] = ms }

    suspend fun setSleepTimerMin(minutes: Int) =
        dataStore.edit { it[PreferencesKeys.SLEEP_TIMER_MIN] = minutes }

    suspend fun setLyricMode(mode: LyricMode) =
        dataStore.edit { it[PreferencesKeys.LYRIC_MODE] = mode.name }
}
