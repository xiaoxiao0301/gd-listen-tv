package com.xiaoxiao0301.amberplay.navigation

import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoxiao0301.amberplay.feature.discover.DiscoverScreen
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerHigh
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceContainerLow
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.feature.favorites.FavoritesScreen
import com.xiaoxiao0301.amberplay.feature.lyrics.LyricsScreen
import com.xiaoxiao0301.amberplay.feature.player.MiniPlayerBar
import com.xiaoxiao0301.amberplay.feature.player.PlayerScreen
import com.xiaoxiao0301.amberplay.feature.player.PlayerViewModel
import com.xiaoxiao0301.amberplay.feature.playlist.PlaylistDetailScreen
import com.xiaoxiao0301.amberplay.feature.playlist.PlaylistListScreen
import com.xiaoxiao0301.amberplay.feature.queue.QueueScreen
import com.xiaoxiao0301.amberplay.feature.search.AlbumDetailScreen
import com.xiaoxiao0301.amberplay.feature.search.ArtistDetailScreen
import com.xiaoxiao0301.amberplay.feature.settings.SettingsScreen
import com.xiaoxiao0301.amberplay.feature.stats.StatsScreen
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import com.xiaoxiao0301.amberplay.AppViewModel
import com.xiaoxiao0301.amberplay.core.common.theme.Amber
import com.xiaoxiao0301.amberplay.core.common.theme.AmberContainer
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurface
import com.xiaoxiao0301.amberplay.core.common.theme.OnSurfaceVariant
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import androidx.compose.ui.draw.clip

private data class NavItem(val route: String, val icon: ImageVector, val label: String)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Home.route, Icons.Outlined.Home, "发现"),
    NavItem(Screen.Playlists.route, Icons.Outlined.LibraryMusic, "歌单"),
    NavItem(Screen.Favorites.route, Icons.Outlined.FavoriteBorder, "收藏"),
    NavItem(Screen.Stats.route, Icons.Outlined.QueryStats, "统计"),
    NavItem(Screen.Settings.route, Icons.Outlined.Settings, "设置"),
)

@Composable
fun AppNavHost() {
    val playerVm: PlayerViewModel = hiltViewModel()
    val appVm: AppViewModel = hiltViewModel()

    val navController = rememberNavController()
    val entry         by navController.currentBackStackEntryAsState()
    val currentRoute  = entry?.destination?.route
    val isOnline      by appVm.isOnline.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
        // ─── 侧边导航栏（全屏播放页和歌词页隐藏导航栏）────────────
        val hideNav = currentRoute == Screen.Player.route || currentRoute == Screen.Lyrics.route
        if (!hideNav) {
            SideNavBar(navController = navController, currentRoute = currentRoute)
        }

        // ─── 主内容区 ────────────────────────────────────────────
        Column(Modifier.weight(1f)) {
            Box(Modifier.weight(1f)) {
                NavHost(navController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) {
                        DiscoverScreen(
                            onSongSelected = { song ->
                                playerVm.playSong(song)
                            },
                            onAlbumClick = { source, albumId ->
                                navController.navigate(
                                    Screen.AlbumDetail.createRoute(
                                        source,
                                        Uri.encode(albumId),
                                    )
                                )
                            },
                            onArtistClick = { source, artistName ->
                                navController.navigate(
                                    Screen.ArtistDetail.createRoute(
                                        source,
                                        Uri.encode(artistName),
                                    )
                                )
                            },
                        )
                    }
                    composable(
                        route = "search?keyword={keyword}",
                        arguments = listOf(
                            navArgument("keyword") {
                                type         = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val initialKeyword = backStackEntry.arguments?.getString("keyword") ?: ""
                        DiscoverScreen(
                            initialKeyword = initialKeyword,
                            onSongSelected = { song ->
                                playerVm.playSong(song)
                            },
                            onAlbumClick = { source, albumId ->
                                navController.navigate(
                                    Screen.AlbumDetail.createRoute(
                                        source,
                                        Uri.encode(albumId),
                                    )
                                )
                            },
                            onArtistClick = { source, artistName ->
                                navController.navigate(
                                    Screen.ArtistDetail.createRoute(
                                        source,
                                        Uri.encode(artistName),
                                    )
                                )
                            },
                        )
                    }
                    composable(Screen.Playlists.route) {
                        PlaylistListScreen(
                            onPlaylistClick = { id ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(id))
                            }
                        )
                    }
                    composable(
                        route     = Screen.PlaylistDetail.ROUTE,
                        arguments = listOf(navArgument("playlistId") { type = NavType.IntType }),
                    ) { back ->
                        PlaylistDetailScreen(
                            playlistId = back.arguments?.getInt("playlistId") ?: 0,
                            onSongSelected = { song ->
                                playerVm.playSong(song)
                            }
                        )
                    }
                    composable(Screen.Favorites.route) {
                        FavoritesScreen(
                            onSongSelected = { song ->
                                playerVm.playSong(song)
                            },
                        )
                    }
                    composable(Screen.Queue.route) {
                        QueueScreen(
                            onSongSelected = { navController.navigate(Screen.Player.route) }
                        )
                    }
                    composable(Screen.Player.route) {
                        PlayerScreen(
                            onClose      = { navController.popBackStack() },
                            onOpenQueue  = { navController.navigate(Screen.Queue.route) },
                        )
                    }
                    composable(Screen.Lyrics.route) {
                        LyricsScreen(
                            onClose = { navController.popBackStack() },
                        )
                    }
                    composable(Screen.Stats.route)    { StatsScreen() }
                    composable(Screen.Settings.route) { SettingsScreen() }
                    // ─── 专辑详情 ──────────────────────────────
                    composable(
                        route     = Screen.AlbumDetail.ROUTE,
                        arguments = listOf(
                            navArgument("source")  { type = NavType.StringType },
                            navArgument("albumId") { type = NavType.StringType },
                        ),
                    ) { back ->
                        AlbumDetailScreen(
                            source  = back.arguments?.getString("source") ?: "",
                            albumId = back.arguments?.getString("albumId") ?: "",
                            onSongSelected = { song ->
                                playerVm.playSong(song)
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    // ─── 歌手详情 ──────────────────────────────
                    composable(
                        route     = Screen.ArtistDetail.ROUTE,
                        arguments = listOf(
                            navArgument("source")     { type = NavType.StringType },
                            navArgument("artistName") { type = NavType.StringType },
                        ),
                    ) { back ->
                        ArtistDetailScreen(
                            source     = back.arguments?.getString("source") ?: "",
                            artistName = Uri.decode(back.arguments?.getString("artistName") ?: ""),
                            onSongSelected = { song ->
                                playerVm.playSong(song)
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }

            // 迷你播放条（全屏播放页 + 歌词页隐藏）
            if (!hideNav) {
                MiniPlayerBar(
                    onExpand = { navController.navigate(Screen.Player.route) },
                    onOpenQueue = { navController.navigate(Screen.Queue.route) },
                )
            }
        }
        } // end Row

        // ─── 离线横幅（全局覆盖顶部） ─────────────────────────────
        if (!isOnline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(SurfaceVariant)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "📵  离线模式 — 仅播放本地缓存歌曲",
                    fontSize  = 14.sp,
                    color     = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    } // end Box
}

@Composable
private fun SideNavBar(navController: NavController, currentRoute: String?) {
    val currentBaseRoute = currentRoute?.substringBefore("?")

    Column(
        modifier = Modifier
            .width(256.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AmberContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(text = "Amber Music", fontSize = 22.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Text(text = "琥珀音乐", fontSize = 11.sp, color = OnSurfaceVariant)
            }
        }

        Spacer(Modifier.height(30.dp))

        NAV_ITEMS.forEach { item ->
            val selected = currentBaseRoute == item.route
            var focused by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) Amber.copy(alpha = 0.22f) else Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .background(
                        when {
                            selected -> AmberContainer.copy(alpha = 0.72f)
                            focused -> SurfaceContainerLow
                            else -> Color.Transparent
                        }
                    )
                    .onFocusChanged { state -> focused = state.isFocused }
                    .focusable()
                    .clickable {
                        if (currentBaseRoute != item.route) {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) Amber else OnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.label,
                    fontSize = 18.sp,
                    color = if (selected) OnSurface else OnSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OnSurfaceVariant.copy(alpha = 0.18f)),
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .border(1.dp, OnSurfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AmberContainer),
                contentAlignment = Alignment.Center,
            ) { Text("A", color = Amber, fontWeight = FontWeight.Bold) }

            Column {
                Text("Amber User", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("Premium Plan", fontSize = 11.sp, color = OnSurfaceVariant)
            }
        }
    }
}



