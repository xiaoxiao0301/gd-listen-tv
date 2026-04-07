package com.xiaoxiao0301.amberplay.core.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF715B30)
val Amber = Color(0xFF8F4D00)
val Purple = Amber
val AmberAccent = Color(0xFFFF9F45)
val AmberContainer = Color(0xFFF5D7A1)
val InfoBlue = Color(0xFF4FC3F7)
val ErrorRed = Color(0xFFBA1A1A)
val CreamBg = Color(0xFFFFF8F0)
val Background = CreamBg
val Surface = Color(0xFFFFF8F0)
val SurfaceContainerLow = Color(0xFFF9F3EA)
val SurfaceContainerHigh = Color(0xFFEEE7DE)
val SurfaceVariant = Color(0xFFE8E2D9)
val OnSurface = Color(0xFF1E1B16)
val OnSurfaceVariant = Color(0xFF4D463B)

private val AmberLightColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    secondary        = InfoBlue,
    onSecondary      = Color.Black,
    tertiary         = Amber,
    onTertiary       = Color.White,
    error            = ErrorRed,
    onError          = Color.White,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onBackground     = OnSurface,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
)

@Composable
fun AmberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmberLightColorScheme,
        typography  = AmberTypography,
        content     = content,
    )
}
