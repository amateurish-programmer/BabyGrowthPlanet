package com.baobei.growthplanet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.baobei.growthplanet.settings.PlanetTheme

data class ThemeArt(val sky: Color, val soft: Color, val accent: Color, val mascot: String, val landscape: String)

fun themeArt(theme: PlanetTheme) = when (theme) {
    PlanetTheme.SPACE -> ThemeArt(Color(0xFFF4F0FF), Color(0xFFE8DFFF), Color(0xFF6C55B7), "🚀", "🪐 ✦ ⭐")
    PlanetTheme.FOREST -> ThemeArt(Color(0xFFF1FAF2), Color(0xFFDDF3DF), Color(0xFF34865B), "🦊", "🌳 🏡 🍃")
    PlanetTheme.RAINBOW -> ThemeArt(Color(0xFFFFF7ED), Color(0xFFFFE5EE), Color(0xFFE75A8E), "🎈", "☁️ 🌈 ☀️")
}

@Composable
fun GrowthPlanetTheme(theme: PlanetTheme, content: @Composable () -> Unit) {
    val art = themeArt(theme)
    val colors = lightColorScheme(
        primary = art.accent,
        onPrimary = Color.White,
        primaryContainer = art.soft,
        onPrimaryContainer = Color(0xFF272031),
        secondary = when (theme) {
            PlanetTheme.SPACE -> Color(0xFFFFB64D)
            PlanetTheme.FOREST -> Color(0xFFF2A65A)
            PlanetTheme.RAINBOW -> Color(0xFF6E83E8)
        },
        background = art.sky,
        surface = Color(0xFFFFFBFF),
        surfaceVariant = art.soft,
        error = Color(0xFFB44545),
    )
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
