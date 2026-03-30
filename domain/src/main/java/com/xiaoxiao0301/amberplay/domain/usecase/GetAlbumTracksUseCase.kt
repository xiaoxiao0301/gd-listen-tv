package com.xiaoxiao0301.amberplay.domain.usecase

import com.xiaoxiao0301.amberplay.domain.model.Song
import com.xiaoxiao0301.amberplay.domain.repository.MusicRepository
import javax.inject.Inject

class GetAlbumTracksUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(albumId: String, source: String): Result<List<Song>> =
        musicRepository.getAlbumTracks(albumId, source)
}
