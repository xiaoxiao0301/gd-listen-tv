package com.xiaoxiao0301.amberplay.core.media

import com.xiaoxiao0301.amberplay.domain.model.Song

/** 播放模式 */
enum class PlayMode { SEQUENTIAL, REPEAT_ONE, REPEAT_ALL, SHUFFLE }

/** 播放状态快照（供 UI 订阅） */
data class PlaybackState(
    val currentSong:  Song?  = null,
    val isPlaying:    Boolean = false,
    val positionMs:   Long    = 0L,
    val durationMs:   Long    = 0L,
    val playMode:     PlayMode = PlayMode.SEQUENTIAL,
    val queueSize:    Int      = 0,
    val currentIndex: Int      = 0,
    val speed:        Float    = 1.0f,
)
