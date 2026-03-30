package com.xiaoxiao0301.amberplay.core.network.mapper

import com.xiaoxiao0301.amberplay.core.network.dto.LyricDto
import com.xiaoxiao0301.amberplay.core.network.dto.SearchResultDto
import com.xiaoxiao0301.amberplay.core.network.dto.SongUrlDto
import com.xiaoxiao0301.amberplay.domain.model.Lyric
import com.xiaoxiao0301.amberplay.domain.model.LyricLine
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.model.SongUrl

fun SearchResultDto.toDomain(): Song = Song(
    id       = "${source}:${id}",
    trackId  = id,
    source   = source,
    name     = name,
    artists  = artist,
    album    = album,
    picId    = picId,
    lyricId  = lyricId,
)

fun SongUrlDto.toDomain(): SongUrl = SongUrl(
    url      = url,
    bitrate  = br,
    sizeKb   = size,
)

private val lrcRegex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})](.*)$""")

private fun parseLrc(lrc: String): Map<Long, String> = buildMap {
    lrc.lines().forEach { line ->
        val match = lrcRegex.matchEntire(line.trim()) ?: return@forEach
        val (min, sec, ms, text) = match.destructured
        val tsMs = min.toLong() * 60_000 +
                   sec.toLong() * 1_000 +
                   ms.padEnd(3, '0').toLong()
        put(tsMs, text.trim())
    }
}

fun LyricDto.toDomain(): Lyric {
    val origMap  = parseLrc(lyric)
    val transMap = tlyric?.let { parseLrc(it) } ?: emptyMap()

    val lines = origMap.entries
        .sortedBy { it.key }
        .map { (ts, text) ->
            LyricLine(
                timestampMs = ts,
                text        = text,
                translation = transMap[ts],
            )
        }
    return Lyric(lines)
}
