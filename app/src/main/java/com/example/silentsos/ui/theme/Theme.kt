package com.example.silentsos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PastelColorScheme = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = PastelPurple,
    onPrimaryContainer = TextDark,
    secondary = PastelPink,
    secondaryContainer = PastelPink,
    onSecondaryContainer = TextDark,
    surfaceVariant = PastelLavender,
    onSurfaceVariant = TextDark,
    tertiary = PastelLavender,
    // contentColorFor() busca el color del fondo en este orden; como los pasteles se
    // repiten entre roles, todos sus "on" deben ser texto oscuro (los de fábrica son blancos).
    onTertiary = TextDark,
    tertiaryContainer = PastelPeach,
    onTertiaryContainer = TextDark,
    background = BackgroundApp,
    surface = SurfaceCard,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = SosRedDark
)

@Composable
fun SilentSOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PastelColorScheme,
        typography = AppTypography,
        content = content
    )
}
