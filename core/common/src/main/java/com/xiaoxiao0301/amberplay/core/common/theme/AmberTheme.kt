package com.xiaoxiao0301.amberplay.core.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF7C5CBF)
val InfoBlue = Color(0xFF4FC3F7)
val ErrorRed = Color(0xFFCF6679)
val Background = Color(0xFF0F0F14)
val Surface = Color(0xFF1A1A24)
val SurfaceVariant = Color(0xFF252535)
val OnSurface = Color(0xFFE8E8F0)
val OnSurfaceVariant = Color(0xFFA0A0B8)

private val AmberDarkColorScheme = darkColorScheme(
    primary          = Purple,
    onPrimary        = Color.White,
    secondary        = InfoBlue,
    onSecondary      = Color.Black,
    error            = ErrorRed,
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
        colorScheme = AmberDarkColorScheme,
        typography  = AmberTypography,
        content     = content,
    )
}
