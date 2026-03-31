package com.xiaoxiao0301.amberplay.feature.playlist

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.domain.model.Playlist
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ─── 歌单列表 ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepo: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepo.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    fun clearExportMessage() { _exportMessage.value = null }

    fun openCreateDialog()  { _showCreateDialog.value = true  }
    fun closeCreateDialog() { _showCreateDialog.value = false }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepo.createPlaylist(name.trim())
            _showCreateDialog.value = false
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch { playlistRepo.deletePlaylist(id) }
    }

    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = playlistRepo.exportPlaylists()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                _exportMessage.value = "已导出"
            }.onFailure {
                _exportMessage.value = "导出失败: ${it.message}"
                Log.e("PlaylistVM", "Export failed", it)
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val jsonContent = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("无法读取文件")
                    // 限制导入文件最大 10 MB，防止超大 JSON 文件导致 OOM
                    val maxBytes = 10L * 1024 * 1024
                    val bytes = input.use { it.readBytes() }
                    require(bytes.size <= maxBytes) { "导入文件过大（上限 10 MB）" }
                    bytes.toString(Charsets.UTF_8)
                }
                playlistRepo.importPlaylists(jsonContent)
                _exportMessage.value = "导入成功"
            }.onFailure {
                _exportMessage.value = "导入失败: ${it.message}"
                Log.e("PlaylistVM", "Import failed", it)
            }
        }
    }
}

// ─── 歌单详情 ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepo: PlaylistRepository,
) : ViewModel() {

    private val _playlistId = MutableStateFlow(0)
    private val _playlist    = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    fun init(playlistId: Int) {
        if (_playlistId.value == playlistId) return
        _playlistId.value = playlistId
        viewModelScope.launch {
            _playlist.value = playlistRepo.getPlaylist(playlistId)
        }
    }

    val songs: StateFlow<List<Song>> = _playlistId
        .flatMapLatest { id ->
            if (id == 0) flowOf(emptyList())
            else playlistRepo.getPlaylistSongs(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun removeSong(songId: String) {
        val pid = _playlistId.value
        if (pid == 0) return
        viewModelScope.launch { playlistRepo.removeSongFromPlaylist(pid, songId) }
    }

    private val _batchRemoveComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val batchRemoveComplete: SharedFlow<Unit> = _batchRemoveComplete.asSharedFlow()

    fun batchRemove(songIds: Set<String>) {
        val pid = _playlistId.value
        if (pid == 0 || songIds.isEmpty()) return
        viewModelScope.launch {
            playlistRepo.batchRemoveSongsFromPlaylist(pid, songIds)
            _batchRemoveComplete.emit(Unit)
        }
    }

}
