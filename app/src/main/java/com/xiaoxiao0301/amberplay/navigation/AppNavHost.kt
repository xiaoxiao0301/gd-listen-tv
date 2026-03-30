package com.xiaoxiao0301.amberplay.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoxiao0301.amberplay.core.common.theme.Purple
import com.xiaoxiao0301.amberplay.core.common.theme.Surface
import com.xiaoxiao0301.amberplay.core.common.theme.SurfaceVariant
import com.xiaoxiao0301.amberplay.feature.favorites.FavoritesScreen
import com.xiaoxiao0301.amberplay.feature.home.HomeScreen
import com.xiaoxiao0301.amberplay.feature.player.MiniPlayerBar
import com.xiaoxiao0301.amberplay.feature.player.PlayerScreen
import com.xiaoxiao0301.amberplay.feature.playlist.PlaylistDetailScreen
import com.xiaoxiao0301.amberplay.feature.playlist.PlaylistListScreen
import com.xiaoxiao0301.amberplay.feature.queue.QueueScreen
import com.xiaoxiao0301.amberplay.feature.search.SearchScreen
import com.xiaoxiao0301.amberplay.feature.settings.SettingsScreen
import com.xiaoxiao0301.amberplay.feature.stats.StatsScreen

private data class NavItem(val route: String, val icon: String, val label: String)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Home.route,      "🏠", "主页"),
    NavItem(Screen.Search.route,    "🔍", "搜索"),
    NavItem(Screen.Playlists.route, "📋", "歌单"),
    NavItem(Screen.Favorites.route, "❤", "收藏"),
    NavItem(Screen.Queue.route,     "⏭", "队列"),
    NavItem(Screen.Stats.route,     "📊", "统计"),
    NavItem(Screen.Settings.route,  "⚙", "设置"),
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val entry         by navController.currentBackStackEntryAsState()
    val currentRoute  = entry?.destination?.route

    Row(Modifier.fillMaxSize()) {
        // ─── 侧边导航栏 ──────────────────────────────────────────
        SideNavBar(navController = navController, currentRoute = currentRoute)

        // ─── 主内容区 ────────────────────────────────────────────
        Column(Modifier.weight(1f)) {
            Box(Modifier.weight(1f)) {
                NavHost(navController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route)      { HomeScreen() }
                    composable(Screen.Search.route)    {
                        SearchScreen(
                            onSongSelected = { navController.navigate(Screen.Player.route) }
                        )
                    }
                    composable(Screen.Playlists.route) { PlaylistListScreen() }
                    composable(
                        route     = Screen.PlaylistDetail.ROUTE,
                        arguments = listOf(navArgument("playlistId") { type = NavType.IntType }),
                    ) { back ->
                        PlaylistDetailScreen(back.arguments?.getInt("playlistId") ?: 0)
                    }
                    composable(Screen.Favorites.route) { FavoritesScreen() }
                    composable(Screen.Queue.route)     { QueueScreen() }
                    composable(Screen.Player.route)    {
                        PlayerScreen(onClose = { navController.popBackStack() })
                    }
                    composable(Screen.Stats.route)     { StatsScreen() }
                    composable(Screen.Settings.route)  { SettingsScreen() }
                }
            }

            // 迷你播放条（全屏播放页隐藏）
            if (currentRoute != Screen.Player.route) {
                MiniPlayerBar(onExpand = { navController.navigate(Screen.Player.route) })
            }
        }
    }
}

@Composable
private fun SideNavBar(navController: NavController, currentRoute: String?) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(if (expanded) 200.dp else 72.dp)
            .fillMaxHeight()
            .background(Surface)
            .onFocusChanged { expanded = it.hasFocus }
            .focusable()
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            selected -> Purple.copy(alpha = 0.2f)
                            else     -> Color.Transparent
                        }
                    )
                    .clickable { navController.navigate(item.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.icon, fontSize = 22.sp)
                if (expanded) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text  = item.label,
                        fontSize = 16.sp,
                        color = if (selected) Purple
                                else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
