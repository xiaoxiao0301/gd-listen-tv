package com.xiaoxiao0301.amberplay.domain.model

data class PlayRecord(
    val id: Long,
    val song: Song,
    val playedAt: Long,
    val durationPlayedMs: Long,
)

data class PlayStat(
    val song: Song,
    val playCount: Int,
    val totalMs: Long,
    val lastPlayed: Long,
)
