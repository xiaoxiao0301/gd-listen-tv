package com.xiaoxiao0301.amberplay.domain.repository

import com.xiaoxiao0301.amberplay.domain.model.Lyric
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.model.SongUrl

interface MusicRepository {
    /** 搜索歌曲；source=null 时使用默认源或多源并行 */
    suspend fun search(keyword: String, source: String? = null, page: Int = 1): Result<List<Song>>

    /** 获取播放 URL */
    suspend fun getSongUrl(song: Song, preferredBr: Int = 999): Result<SongUrl>

    /** 获取歌词 */
    suspend fun getLyric(song: Song): Result<Lyric>

    /** 获取专辑曲目列表（source_album 模式） */
    suspend fun getAlbumTracks(albumId: String, source: String): Result<List<Song>>
}
