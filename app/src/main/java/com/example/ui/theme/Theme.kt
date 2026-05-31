package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CyberCyan,
    secondary = SolarPurple,
    tertiary = EmeraldActive,
    background = DarkCosmicSlate,
    surface = TechCardBg,
    onBackground = IceWhite,
    onSurface = IceWhite,
    primaryContainer = DeepIndigoBg,
    onPrimaryContainer = CyberCyan,
    surfaceVariant = DeepIndigoBg,
    onSurfaceVariant = PaleBlueGrey,
    outline = BorderSlate
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Emphasize light theme for Professional Polish
    dynamicColor: Boolean = false, // Keep signature custom branding across all OS versions
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

