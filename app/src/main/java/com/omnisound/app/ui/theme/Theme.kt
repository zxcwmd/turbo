package com.omnisound.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Mono = darkColorScheme(
    primary = Color(0xFFF4F4F4), onPrimary = Color(0xFF111111),
    secondary = Color(0xFFB7B7B7), onSecondary = Color(0xFF111111),
    background = Color(0xFF080808), onBackground = Color(0xFFF4F4F4),
    surface = Color(0xFF141414), onSurface = Color(0xFFF4F4F4),
    surfaceVariant = Color(0xFF202020), onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF393939)
)

@Composable fun OmniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Mono, content = content)
}
