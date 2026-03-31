package com.xiaoxiao0301.amberplay.core.network.repository

import com.xiaoxiao0301.amberplay.core.datastore.SettingsDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val api:           MusicApiService,
    private val settingsStore: SettingsDataStore,
) : MusicRepository {

    override suspend fun search(
        keyword: String,
        source: String?,
        page: Int,
    ): Result<List<Song>> = runCatching {
        withContext(Dispatchers.IO) {
            val sources: List<String> = if (source != null) {
                listOf(source)
            } else {
                val settings = settingsStore.settings.first()
                if (settings.multiSourceSearch) {
                    settings.enabledSources.toList().ifEmpty { listOf(settings.defaultSource) }
                } else {
                    listOf(settings.defaultSource)
                }
            }
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
                val settings = settingsStore.settings.first()
                // Build a source order: preferred source first, then remaining enabled sources
                val fallbacks = (listOf(song.source) +
                        settings.enabledSources.filter { it != song.source })
                    .take(3)   // at most 3 attempts total

                var lastResult: SongUrl? = null
                var lastException: Throwable? = null
                for (src in fallbacks) {
                    try {
                        val songUrl = api.getSongUrl(
                            source = src,
                            id     = song.trackId,
                            br     = preferredBr,
                        ).toDomain()
                        if (songUrl.url.isNotBlank()) return@withContext songUrl
                        lastResult = songUrl  // blank – try next source
                    } catch (e: Exception) {
                        lastException = e
                    }
                }
                if (lastException != null) throw lastException
                // All sources returned blank URL — return the last blank result (no extra network call)
                lastResult ?: error("No song URL found for ${song.id}")
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
