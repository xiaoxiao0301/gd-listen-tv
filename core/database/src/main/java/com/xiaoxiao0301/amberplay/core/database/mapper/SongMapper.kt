package com.xiaoxiao0301.amberplay.core.database.mapper

import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import com.xiaoxiao0301.amberplay.domain.model.Song
import org.json.JSONArray

fun Song.toEntity(existingCreatedAt: Long? = null): SongEntity = SongEntity(
    id         = id,
    trackId    = trackId,
    source     = source,
    name       = name,
    artists    = JSONArray(artists).toString(),
    album      = album,
    picId      = picId,
    lyricId    = lyricId,
    durationMs = durationMs,
    // 保留已存在记录的创建时间；新记录才使用当前时间
    createdAt  = existingCreatedAt ?: System.currentTimeMillis(),
)

/**
 * Upsert a [Song] while preserving the original [SongEntity.createdAt] if the record already exists.
 */
suspend fun SongDao.upsertPreserving(song: Song) {
    val existing = getById(song.id)
    upsert(song.toEntity(existingCreatedAt = existing?.createdAt))
}

fun SongEntity.toDomain(): Song = Song(
    id         = id,
    trackId    = trackId,
    source     = source,
    name       = name,
    artists    = parseArtists(artists),
    album      = album,
    picId      = picId,
    lyricId    = lyricId,
    durationMs = durationMs,
)

private fun parseArtists(json: String): List<String> = runCatching {
    val arr = JSONArray(json)
    List(arr.length()) { arr.getString(it) }
}.getOrDefault(listOf(json))
