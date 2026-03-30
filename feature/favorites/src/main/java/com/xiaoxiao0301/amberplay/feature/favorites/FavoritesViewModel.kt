package com.xiaoxiao0301.amberplay.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepo: FavoriteRepository,
) : ViewModel() {

    val favorites: StateFlow<List<Song>> = favoriteRepo.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun removeFavorite(song: Song) {
        viewModelScope.launch { favoriteRepo.removeFavorite(song.id) }
    }

    fun batchRemoveFavorites(ids: Set<String>) {
        viewModelScope.launch { favoriteRepo.batchRemoveFavorites(ids.toList()) }
    }
}
