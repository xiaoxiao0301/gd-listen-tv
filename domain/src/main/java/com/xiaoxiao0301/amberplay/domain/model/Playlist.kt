package com.xiaoxiao0301.amberplay.domain.model

data class Playlist(
    val id: Int,
    val name: String,
    val description: String = "",
    val coverSongId: String? = null,
    val songCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)
