package com.xiaoxiao0301.amberplay.core.network.repository

import com.xiaoxiao0301.amberplay.core.network.api.MusicApiService
import com.xiaoxiao0301.amberplay.core.network.mapper.toDomain
import com.xiaoxiao0301.amberplay.domain.model.Lyric
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.model.SongUrl
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val api: MusicApiService,
) : MusicRepository {

    private val stableSources = listOf("netease", "kuwo", "joox", "bilibili")

    override suspend fun search(
        keyword: String,
        source: String?,
        page: Int,
    ): Result<List<Song>> = runCatching {
        withContext(Dispatchers.IO) {
            val sources = if (source != null) listOf(source) else listOf("netease")
            coroutineScope {
                sources.map { src ->
                    async {
                        runCatching { api.search(source = src, name = keyword, pages = page) }
                            .getOrDefault(emptyList())
                    }
                }.awaitAll()
                    .flatten()
                    .distinctBy { "${it.source}:${it.id}" }
                    .map { it.toDomain() }
            }
        }
    }

    override suspend fun getSongUrl(song: Song, preferredBr: Int): Result<SongUrl> =
        runCatching {
            withContext(Dispatchers.IO) {
                api.getSongUrl(source = song.source, id = song.trackId, br = preferredBr)
                    .toDomain()
            }
        }

    override suspend fun getLyric(song: Song): Result<Lyric> =
        runCatching {
            withContext(Dispatchers.IO) {
                api.getLyric(source = song.source, id = song.lyricId)
                    .toDomain()
            }
        }

    override suspend fun getAlbumTracks(albumId: String, source: String): Result<List<Song>> =
        runCatching {
            withContext(Dispatchers.IO) {
                api.search(source = "${source}_album", name = albumId)
                    .map { it.toDomain() }
            }
        }
}
