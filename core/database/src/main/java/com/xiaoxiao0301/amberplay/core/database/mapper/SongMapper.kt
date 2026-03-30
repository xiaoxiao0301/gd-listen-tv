package com.xiaoxiao0301.amberplay.core.database.mapper

import com.xiaoxiao0301.amberplay.core.database.entity.SongEntity
import com.xiaoxiao0301.amberplay.domain.model.Song
import org.json.JSONArray

fun Song.toEntity(): SongEntity = SongEntity(
    id         = id,
    trackId    = trackId,
    source     = source,
    name       = name,
    artists    = JSONArray(artists).toString(),
    album      = album,
    picId      = picId,
    lyricId    = lyricId,
    durationMs = durationMs,
    createdAt  = System.currentTimeMillis(),
)

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
