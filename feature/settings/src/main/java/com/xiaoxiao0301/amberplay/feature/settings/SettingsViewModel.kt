package com.xiaoxiao0301.amberplay.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.cache.AudioCache
import com.xiaoxiao0301.amberplay.core.datastore.AppSettings
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.network.ratelimit.RateLimiter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDs: SettingsDataStore,
    private val audioCache: AudioCache,
    private val rateLimiter: RateLimiter,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _cacheUsedMb = MutableStateFlow(audioCache.usedBytes() / (1024L * 1024L))
    val cacheUsedMb: StateFlow<Long> = _cacheUsedMb.asStateFlow()

    private val _remainingTokens = MutableStateFlow(rateLimiter.remainingTokens)
    val remainingTokens: StateFlow<Int> = _remainingTokens.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(2_000L)
                _remainingTokens.value = rateLimiter.remainingTokens
            }
        }
    }

    fun setBitrate(br: Int) = viewModelScope.launch { settingsDs.setBitrate(br) }

    fun setDefaultSource(source: String) = viewModelScope.launch {
        settingsDs.setDefaultSource(source)
    }

    fun setMultiSourceSearch(enabled: Boolean) = viewModelScope.launch {
        settingsDs.setMultiSourceSearch(enabled)
    }

    fun toggleEnabledSource(source: String) = viewModelScope.launch {
        val current = settings.value.enabledSources.toMutableSet()
        if (source in current && current.size > 1) {
            current.remove(source)
        } else {
            current.add(source)
        }
        settingsDs.setEnabledSources(current)
    }

    fun setLyricMode(mode: LyricMode) = viewModelScope.launch {
        settingsDs.setLyricMode(mode)
    }

    fun setPlaybackSpeed(speed: Float) = viewModelScope.launch {
        settingsDs.setPlaybackSpeed(speed)
    }

    fun setSleepTimerMin(minutes: Int) = viewModelScope.launch {
        settingsDs.setSleepTimerMin(minutes)
    }

    fun setCrossfadeMs(ms: Int) = viewModelScope.launch {
        settingsDs.setCrossfadeMs(ms)
    }

    fun setCacheLimitMb(mb: Int) = viewModelScope.launch {
        settingsDs.setCacheLimitMb(mb)
        audioCache.updateLimit(mb)
    }

    fun clearCache() {
        audioCache.clear()
        _cacheUsedMb.value = 0L
    }

    fun setOfflineMode(enabled: Boolean) = viewModelScope.launch {
        settingsDs.setOfflineMode(enabled)
    }
}
