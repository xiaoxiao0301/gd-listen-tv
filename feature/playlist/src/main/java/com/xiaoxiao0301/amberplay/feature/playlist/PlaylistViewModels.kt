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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
                withContext(Dispatchers.IO) {
                    val tmp = File(context.cacheDir, "import_${System.currentTimeMillis()}.json")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { output -> input.copyTo(output) }
                    }
                    playlistRepo.importPlaylists(tmp)
                    tmp.delete()
                }
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

    fun init(playlistId: Int) {
        if (_playlistId.value == playlistId) return
        _playlistId.value = playlistId
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

}
