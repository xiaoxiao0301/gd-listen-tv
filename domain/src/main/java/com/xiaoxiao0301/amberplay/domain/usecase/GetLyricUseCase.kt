package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.Lyric
import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import javax.inject.Inject

class GetLyricUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(song: Song): Result<Lyric> =
        musicRepository.getLyric(song)
}
