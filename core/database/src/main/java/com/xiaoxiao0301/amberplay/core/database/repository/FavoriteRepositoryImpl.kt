package com.xiaoxiao0301.amberplay.core.database.repository

import com.xiaoxiao0301.amberplay.core.database.dao.FavoriteDao
import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.FavoriteEntity
import com.xiaoxiao0301.amberplay.core.database.mapper.toDomain
import com.xiaoxiao0301.amberplay.core.database.mapper.toEntity
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao,
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<Song>> =
        favoriteDao.getFavoriteSongs().map { list -> list.map { it.toDomain() } }

    override suspend fun addFavorite(song: Song) {
        songDao.upsert(song.toEntity())
        favoriteDao.addFavorite(
            FavoriteEntity(songId = song.id, addedAt = System.currentTimeMillis())
        )
    }

    override suspend fun removeFavorite(songId: String) {
        favoriteDao.removeFavorite(songId)
    }

    override suspend fun batchRemoveFavorites(songIds: List<String>) {
        favoriteDao.batchRemoveFavorites(songIds)
    }

    override fun isFavorite(songId: String): Flow<Boolean> =
        favoriteDao.isFavoriteFlow(songId).map { it > 0 }
}
