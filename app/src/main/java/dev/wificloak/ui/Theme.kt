package dev.wificloak.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF275D4E), onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E9DD), onPrimaryContainer = Color(0xFF153A2C),
    secondary = Color(0xFF526557), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E9DE), onSecondaryContainer = Color(0xFF273629),
    tertiary = Color(0xFF596548), tertiaryContainer = Color(0xFFE0E8C8),
    background = Color(0xFFF6F3EA), onBackground = Color(0xFF202720),
    surface = Color(0xFFFFFEF8), onSurface = Color(0xFF202720),
    surfaceVariant = Color(0xFFE5E8DC), onSurfaceVariant = Color(0xFF465345),
    surfaceContainer = Color(0xFFEEEEE3), surfaceContainerLow = Color(0xFFF5F4EC),
    surfaceContainerHigh = Color(0xFFE8EADD), surfaceContainerHighest = Color(0xFFE1E5D8),
    outline = Color(0xFF738174), outlineVariant = Color(0xFFC5CDBC),
    error = Color(0xFFA33830), errorContainer = Color(0xFFFFDAD4), onErrorContainer = Color(0xFF541D18)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9D0B8), onPrimary = Color(0xFF11382A),
    primaryContainer = Color(0xFF284D3B), onPrimaryContainer = Color(0xFFD7EDDE),
    secondary = Color(0xFFBBCBB8), secondaryContainer = Color(0xFF354333),
    onSecondaryContainer = Color(0xFFDFEBD9), tertiary = Color(0xFFC2CEA3),
    background = Color(0xFF111812), onBackground = Color(0xFFE1E8DC),
    surface = Color(0xFF192019), onSurface = Color(0xFFE1E8DC),
    surfaceVariant = Color(0xFF3B473A), onSurfaceVariant = Color(0xFFC1CCB9),
    surfaceContainer = Color(0xFF20281F), surfaceContainerLow = Color(0xFF1A211A),
    surfaceContainerHigh = Color(0xFF293126), surfaceContainerHighest = Color(0xFF343D30),
    outline = Color(0xFF8B998A), outlineVariant = Color(0xFF465342),
    error = Color(0xFFFFB4A8), errorContainer = Color(0xFF74332A), onErrorContainer = Color(0xFFFFDAD4)
)

@Composable
fun CloakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(), content = content)
}
