package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.model.SongUrl
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import javax.inject.Inject

class GetSongUrlUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(song: Song, preferredBr: Int = 999): Result<SongUrl> =
        musicRepository.getSongUrl(song, preferredBr)
}
