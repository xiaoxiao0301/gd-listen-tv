package com.xiaoxiao0301.amberplay.domain.model

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translation: String? = null,
)

data class Lyric(
    val lines: List<LyricLine>,
)
