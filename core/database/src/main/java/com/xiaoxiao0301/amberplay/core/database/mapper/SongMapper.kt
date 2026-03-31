package com.xiaoxiao0301.amberplay.core.database.mapper

import com.xiaoxiao0301.amberplay.core.database.dao.SongDao
import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import com.xiaoxiao0301.amberplay.domain.model.Song

fun Song.toEntity(existingCreatedAt: Long? = null): SongEntity = SongEntity(
    id         = id,
    trackId    = trackId,
    source     = source,
    name       = name,
    artists    = artists.encodeJsonStringArray(),
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

/** Serialise a [List<String>] to a compact JSON array — pure Kotlin, no Android deps. */
private fun List<String>.encodeJsonStringArray(): String =
    joinToString(",", "[", "]") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }

/** Deserialise a JSON string array produced by [encodeJsonStringArray]. */
private fun parseArtists(json: String): List<String> = runCatching {
    val inner = json.trim().removePrefix("[").removeSuffix("]").trim()
    if (inner.isBlank()) return@runCatching emptyList()
    Regex(""""((?:[^"\\]|\\.)*)"""").findAll(inner)
        .map { it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\") }
        .toList()
}.getOrDefault(listOf(json))

