package com.baobei.growthplanet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baobei.growthplanet.ui.AppViewModel
import com.baobei.growthplanet.ui.components.RoundedPanel

@Composable
fun RewardsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val rewards by vm.rewards.collectAsStateWithLifecycle()
    val redemptions by vm.redemptions.collectAsStateWithLifecycle()
    val balance by vm.balance.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("奖励乐园", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text("用成长积分兑换温暖时光") }
            IconButton({ showHistory = true }) { Icon(Icons.Default.History, "兑换记录") }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 44.sp); Spacer(Modifier.width(14.dp)); Column { Text("可用成长积分"); Text("$balance", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item { Text("和家长一起选一个喜欢的奖励吧", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(rewards, key = { it.id }) { reward ->
                Card(shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(reward.emoji, fontSize = 36.sp); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(reward.title, fontWeight = FontWeight.Bold); Text("${reward.cost} 积分", color = MaterialTheme.colorScheme.primary) }
                        Button({ vm.redeem(reward) { message = if (it) "兑换成功，一起开心兑现吧！" else "积分还差一点，继续积累星光吧！" } }, enabled = balance >= reward.cost) { Text("兑换") }
                    }
                }
            }
            item {
                RoundedPanel {
                    Text("💡 非物质奖励更有温度", fontWeight = FontWeight.Bold)
                    Text("可以试试：睡前多讲一个故事、一起做饭、选择周末路线、亲子桌游、去图书馆。", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton({ message = null }) { Text("知道啦") } }, title = { Text("成长星光") }, text = { Text(it) }) }
    if (showHistory) AlertDialog(onDismissRequest = { showHistory = false }, confirmButton = { TextButton({ showHistory = false }) { Text("关闭") } }, title = { Text("兑换记录") }, text = { Column { if (redemptions.isEmpty()) Text("还没有兑换记录") else redemptions.take(8).forEach { Text("${it.rewardTitleSnapshot}  −${it.costSnapshot} 分", Modifier.padding(vertical = 5.dp)) } } })
}
