package com.xiaoxiao0301.amberplay.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResultDto(
    @Json(name = "id")       val id: String,
    @Json(name = "name")     val name: String,
    @Json(name = "artist")   val artist: List<String>,
    @Json(name = "album")    val album: String,
    @Json(name = "pic_id")   val picId: String,
    @Json(name = "url_id")   val urlId: String,
    @Json(name = "lyric_id") val lyricId: String,
    @Json(name = "source")   val source: String,
)

@JsonClass(generateAdapter = true)
data class SongUrlDto(
    @Json(name = "url")  val url: String,
    @Json(name = "br")   val br: Int,
    @Json(name = "size") val size: Long,
)

@JsonClass(generateAdapter = true)
data class PicUrlDto(
    @Json(name = "url") val url: String,
)

@JsonClass(generateAdapter = true)
data class LyricDto(
    @Json(name = "lyric")  val lyric: String,
    @Json(name = "tlyric") val tlyric: String?,
)
