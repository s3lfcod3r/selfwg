package com.selfwg.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF19C3B1)
private val TealDark = Color(0xFF0E8C80)
private val Bg = Color(0xFF0E1413)
private val SurfaceColor = Color(0xFF16201E)
private val OnColor = Color(0xFFE6F2F0)

private val SelfColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF03110F),
    secondary = TealDark,
    onSecondary = Color(0xFF03110F),
    background = Bg,
    onBackground = OnColor,
    surface = SurfaceColor,
    onSurface = OnColor,
    surfaceVariant = Color(0xFF1E2A28),
    onSurfaceVariant = Color(0xFFB6C7C4),
)

@Composable
fun SelfWgTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SelfColors, content = content)
}
