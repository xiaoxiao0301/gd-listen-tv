package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import javax.inject.Inject

class SearchMusicUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(
        keyword: String,
        source: String? = null,
        page: Int = 1,
    ): Result<List<Song>> = musicRepository.search(keyword, source, page)
}
