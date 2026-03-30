package com.xiaoxiao0301.amberplay.core.network.api

import com.xiaoxiao0301.amberplay.core.network.dto.LyricDto
import com.xiaoxiao0301.amberplay.core.network.dto.PicUrlDto
import com.xiaoxiao0301.amberplay.core.network.dto.SearchResultDto
import com.xiaoxiao0301.amberplay.core.network.dto.SongUrlDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApiService {

    @GET("api.php")
    suspend fun search(
        @Query("types")  types:  String = "search",
        @Query("source") source: String = "netease",
        @Query("name")   name:   String,
        @Query("count")  count:  Int    = 20,
        @Query("pages")  pages:  Int    = 1,
    ): List<SearchResultDto>

    @GET("api.php")
    suspend fun getSongUrl(
        @Query("types")  types:  String = "url",
        @Query("source") source: String = "netease",
        @Query("id")     id:     String,
        @Query("br")     br:     Int    = 999,
    ): SongUrlDto

    @GET("api.php")
    suspend fun getPicUrl(
        @Query("types")  types:  String = "pic",
        @Query("source") source: String = "netease",
        @Query("id")     id:     String,
        @Query("size")   size:   Int    = 500,
    ): PicUrlDto

    @GET("api.php")
    suspend fun getLyric(
        @Query("types")  types:  String = "lyric",
        @Query("source") source: String = "netease",
        @Query("id")     id:     String,
    ): LyricDto
}
