package com.baobei.growthplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onCreate: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf("🚀") }
    val avatars = listOf("🚀", "🦊", "🎈", "🐼", "🐰")
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🪐", fontSize = 78.sp)
        Text("欢迎来到宝贝成长星球", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("每天看见一点进步，一起收集成长星光", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(name, { name = it.take(12) }, label = { Text("宝贝的小名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Text("选择成长伙伴", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            avatars.forEach { item ->
                FilterChip(selected = avatar == item, onClick = { avatar = item }, label = { Text(item, fontSize = 25.sp) }, shape = RoundedCornerShape(18.dp))
            }
        }
        Spacer(Modifier.height(26.dp))
        Button(onClick = { onCreate(name, avatar) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Text("开启成长旅程", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("数据仅保存在设备及你自己的 Android 系统备份中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
