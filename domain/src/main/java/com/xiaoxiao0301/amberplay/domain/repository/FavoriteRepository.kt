package com.xiaoxiao0301.amberplay.domain.repository

import com.xiaoxiao0301.amberplay.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(): Flow<List<Song>>
    suspend fun addFavorite(song: Song)
    suspend fun removeFavorite(songId: String)
    suspend fun batchRemoveFavorites(songIds: List<String>)
    fun isFavorite(songId: String): Flow<Boolean>
}
