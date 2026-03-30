package com.xiaoxiao0301.amberplay.domain.model

data class Song(
    /** 全局唯一ID: "{source}:{trackId}" */
    val id: String,
    val trackId: String,
    val source: String,
    val name: String,
    val artists: List<String>,
    val album: String,
    val picId: String,
    val lyricId: String,
    val durationMs: Long = 0L,
) {
    val artistText: String get() = artists.joinToString(" / ")
}
