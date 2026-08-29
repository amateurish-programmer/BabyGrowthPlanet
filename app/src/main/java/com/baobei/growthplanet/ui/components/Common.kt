package com.baobei.growthplanet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baobei.growthplanet.data.ChildProfileEntity
import com.baobei.growthplanet.ui.theme.themeArt
import com.baobei.growthplanet.settings.PlanetTheme

@Composable
fun PlanetHeader(
    title: String,
    subtitle: String,
    child: ChildProfileEntity?,
    theme: PlanetTheme,
    musicEnabled: Boolean,
    onMusicToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val art = themeArt(theme)
    Column(modifier.background(art.soft).statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(child?.avatar ?: art.mascot, fontSize = 38.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = onMusicToggle) {
                Icon(if (musicEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, contentDescription = if (musicEnabled) "关闭音乐" else "打开音乐")
            }
        }
        Text(art.landscape, fontSize = 21.sp, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun RoundedPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun ScoreProgress(value: Float, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { value.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(10.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.15f),
    )
}
