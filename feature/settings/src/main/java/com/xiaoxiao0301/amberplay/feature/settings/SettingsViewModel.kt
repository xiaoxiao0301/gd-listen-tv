package com.xiaoxiao0301.amberplay.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.datastore.AppSettings
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDs: SettingsDataStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

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

    fun setCacheLimitMb(mb: Int) = viewModelScope.launch {
        settingsDs.setCacheLimitMb(mb)
    }

    fun setOfflineMode(enabled: Boolean) = viewModelScope.launch {
        settingsDs.setOfflineMode(enabled)
    }
}
