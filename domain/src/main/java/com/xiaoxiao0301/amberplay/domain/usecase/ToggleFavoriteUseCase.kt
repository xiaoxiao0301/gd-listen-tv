package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend operator fun invoke(song: Song, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteRepository.removeFavorite(song.id)
        } else {
            favoriteRepository.addFavorite(song)
        }
    }
}
