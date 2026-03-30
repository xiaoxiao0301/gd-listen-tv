package com.xiaoxiao0301.amberplay.navigation

/** 全局路由定义 */
sealed class Screen(val route: String) {
    object Home      : Screen("home")
    object Search    : Screen("search")
    object Playlists : Screen("playlists")
    object Favorites : Screen("favorites")
    object Queue     : Screen("queue")
    object Player    : Screen("player")
    object Lyrics    : Screen("lyrics")
    object Stats     : Screen("stats")
    object Settings  : Screen("settings")

    data class PlaylistDetail(val dummy: Unit = Unit) : Screen("playlist_detail/{playlistId}") {
        companion object {
            const val ROUTE = "playlist_detail/{playlistId}"
            fun createRoute(id: Int) = "playlist_detail/$id"
        }
    }

    data class AlbumDetail(val dummy: Unit = Unit) : Screen("album_detail/{source}/{albumId}") {
        companion object {
            const val ROUTE = "album_detail/{source}/{albumId}"
            fun createRoute(source: String, albumId: String) =
                "album_detail/${source}/${albumId}"
        }
    }
}
