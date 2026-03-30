package com.xiaoxiao0301.amberplay.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.core.datastore.LyricMode
import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
import com.xiaoxiao0301.amberplay.core.media.PlaybackState
import com.xiaoxiao0301.amberplay.core.media.PlayerController
import com.xiaoxiao0301.amberplay.domain.model.Lyric
import com.xiaoxiao0301.amberplay.domain.model.LyricLine
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.usecase.GetLyricUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LyricsUiState {
    object Loading : LyricsUiState()
    object NoLyric : LyricsUiState()
    data class Ready(val lines: List<LyricLine>, val mode: LyricMode) : LyricsUiState()
    data class Error(val msg: String) : LyricsUiState()
}

@HiltViewModel
class LyricsViewModel @Inject constructor(
    val playerController: PlayerController,
    private val getLyric: GetLyricUseCase,
    private val settingsDs: SettingsDataStore,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState())

    private val _uiState = MutableStateFlow<LyricsUiState>(LyricsUiState.Loading)
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private val _lyricMode = MutableStateFlow(LyricMode.BILINGUAL)
    val lyricMode: StateFlow<LyricMode> = _lyricMode.asStateFlow()

    private var lastSongId: String? = null

    init {
        viewModelScope.launch {
            _lyricMode.value = settingsDs.settings.first().lyricMode
        }
        viewModelScope.launch {
            playerController.state.collect { state ->
                val song = state.currentSong ?: return@collect
                if (song.id != lastSongId) {
                    lastSongId = song.id
                    loadLyric(song)
                }
            }
        }
    }

    private suspend fun loadLyric(song: Song) {
        _uiState.value = LyricsUiState.Loading
        getLyric(song)
            .onSuccess { lyric ->
                if (lyric.lines.isEmpty()) {
                    _uiState.value = LyricsUiState.NoLyric
                } else {
                    _uiState.value = LyricsUiState.Ready(lyric.lines, _lyricMode.value)
                }
            }
            .onFailure { _uiState.value = LyricsUiState.NoLyric }
    }

    /** 当前应高亮的歌词行下标 */
    val currentLineIndex: StateFlow<Int> = playerController.state
        .map { state ->
            val ready = _uiState.value as? LyricsUiState.Ready ?: return@map 0
            val pos   = state.positionMs
            val idx   = ready.lines.indexOfLast { it.timestampMs <= pos }
            if (idx < 0) 0 else idx
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** 跳转到指定歌词行对应的播放位置 */
    fun seekToLine(timestampMs: Long) {
        playerController.seekTo(timestampMs)
    }

    fun cycleMode() {
        val next = when (_lyricMode.value) {
            LyricMode.ORIGINAL     -> LyricMode.TRANSLATION
            LyricMode.TRANSLATION  -> LyricMode.BILINGUAL
            LyricMode.BILINGUAL    -> LyricMode.ORIGINAL
        }
        _lyricMode.value = next
        viewModelScope.launch { settingsDs.setLyricMode(next) }
        // 重新包装 Ready state
        val current = _uiState.value
        if (current is LyricsUiState.Ready) {
            _uiState.value = current.copy(mode = next)
        }
    }
}
