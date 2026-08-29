package com.baobei.growthplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baobei.growthplanet.data.BehaviorItemEntity
import com.baobei.growthplanet.data.DeductionTypeEntity
import com.baobei.growthplanet.domain.GrowthCalculator
import com.baobei.growthplanet.settings.PlanetTheme
import com.baobei.growthplanet.ui.AppViewModel
import com.baobei.growthplanet.ui.components.PlanetHeader
import com.baobei.growthplanet.ui.components.RoundedPanel
import com.baobei.growthplanet.ui.components.ScoreProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TodayScreen(vm: AppViewModel, theme: PlanetTheme, modifier: Modifier = Modifier) {
    val child by vm.selectedChild.collectAsStateWithLifecycle()
    val music by vm.musicEnabled.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val todayError by vm.todayError.collectAsStateWithLifecycle()
    val types by vm.deductionTypes.collectAsStateWithLifecycle()
    var proud by rememberSaveable { mutableStateOf("") }
    var encouragement by rememberSaveable { mutableStateOf("") }
    var parentMode by rememberSaveable { mutableStateOf(false) }
    var deductionMenu by remember { mutableStateOf(false) }

    LaunchedEffect(today?.record?.id) {
        proud = today?.record?.proudMoment.orEmpty()
        encouragement = today?.record?.encouragement.orEmpty()
        parentMode = today?.record?.confirmed == true
    }

    Column(modifier.fillMaxSize()) {
        PlanetHeader(
            title = "${child?.name.orEmpty()}的今日成长",
            subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE")),
            child = child,
            theme = theme,
            musicEnabled = music,
            onMusicToggle = { vm.setMusic(!music) },
        )
        val data = today
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (todayError == null) {
                    CircularProgressIndicator()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("今日成长读取失败", fontWeight = FontWeight.Bold)
                        Text(todayError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = vm::loadToday, modifier = Modifier.padding(top = 12.dp)) { Text("重新读取") }
                    }
                }
            }
            return@Column
        }
        val activeItems = data.categories.flatMap { group ->
            group.items.filter { it.enabled && !it.deleted && group.category.enabled }.map { group.category to it }
        }
        val positive = activeItems.sumOf { (_, item) ->
            data.scores[item.id]?.let { if (it.parentScore >= 0) it.parentScore else it.selfScore.coerceAtLeast(0) } ?: 0
        }
        val maxScore = activeItems.sumOf { it.second.maxScore }
        val deduction = data.deductions.sumOf { it.pointsSnapshot * it.count }
        val finalScore = GrowthCalculator.finalScore(positive, deduction)
        val stars = GrowthCalculator.stars(finalScore, maxScore)

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                RoundedPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val heading = when {
                                data.record.confirmed -> "今天已完成"
                                parentMode -> "请家长一起确认"
                                else -> "先听听宝贝怎么说"
                            }
                            Text(heading, fontWeight = FontWeight.Bold)
                            Text("$finalScore / $maxScore 分  ${"★".repeat(stars)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (data.record.confirmed) {
                            OutlinedButton(onClick = vm::resetToday, shape = RoundedCornerShape(14.dp)) {
                                Text("重新打分")
                            }
                        } else {
                            AssistChip(onClick = { parentMode = !parentMode }, label = { Text(if (parentMode) "返回自评" else "家长确认") })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    ScoreProgress(GrowthCalculator.percent(finalScore, maxScore))
                    Text("星级随启用项目的满分动态计算，记录坚持比天天满分更重要。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
            data.categories.forEach { group ->
                val visible = group.items.filter { it.enabled && !it.deleted }
                if (visible.isNotEmpty() && group.category.enabled) {
                    item { Text("${group.category.emoji}  ${group.category.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                    items(visible, key = { it.id }) { behavior ->
                        val score = data.scores[behavior.id]?.let {
                            if (parentMode) it.parentScore.takeIf { value -> value >= 0 } else it.selfScore.takeIf { value -> value >= 0 }
                        }
                        BehaviorScoreCard(behavior, score, data.record.confirmed, parentMode) { vm.score(behavior.id, it, parentMode) }
                    }
                }
            }
            item {
                RoundedPanel {
                    Text("需要复盘的小插曲", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("只记录发生了什么，一起想下次可以怎样做。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    data.deductions.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${entry.titleSnapshot}  −${entry.pointsSnapshot}", Modifier.weight(1f))
                            IconButton(onClick = { vm.removeDeduction(entry.id) }, enabled = !data.record.confirmed) { Icon(Icons.Default.Delete, "移除") }
                        }
                    }
                    OutlinedButton(onClick = { deductionMenu = true }, enabled = !data.record.confirmed) {
                        Text("扣分项")
                    }
                }
            }
            item {
                RoundedPanel {
                    Text("今天最骄傲的事", fontWeight = FontWeight.Bold)
                    OutlinedTextField(proud, { proud = it.take(100) }, placeholder = { Text("例如：今天主动收好了玩具") }, enabled = !data.record.confirmed, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text("给宝贝的鼓励", fontWeight = FontWeight.Bold)
                    OutlinedTextField(encouragement, { encouragement = it.take(100) }, placeholder = { Text("我看见你一直在努力！") }, enabled = !data.record.confirmed, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    val complete = activeItems.all { data.scores[it.second.id]?.selfScore ?: -1 >= 0 }
                    Button(
                        onClick = { vm.confirm(proud, encouragement) },
                        enabled = !data.record.confirmed && complete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("家长确认并收下今天的星光")
                    }
                    if (!data.record.confirmed) Text("完成全部孩子自评后即可确认；家长可先切换确认模式调整。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    if (deductionMenu) {
        DeductionPickerDialog(
            types = types,
            onDismiss = { deductionMenu = false },
            onSelect = { type ->
                vm.addDeduction(type)
                deductionMenu = false
            },
        )
    }
}

@Composable
internal fun DeductionPickerDialog(
    types: List<DeductionTypeEntity>,
    onDismiss: () -> Unit,
    onSelect: (DeductionTypeEntity) -> Unit,
) {
    val listState = rememberLazyListState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择扣分项") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        text = {
            if (types.isEmpty()) {
                Text("还没有可用的扣分项，可在“我的－管理行为项目”中添加。")
            } else {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                    ) {
                        items(types, key = { it.id }) { type ->
                            TextButton(
                                onClick = { onSelect(type) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(type.title, Modifier.weight(1f))
                                Text("−${type.points} 分", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    DeductionListScrollIndicator(
                        listState = listState,
                        totalItems = types.size,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }
            }
        },
    )
}

@Composable
private fun DeductionListScrollIndicator(listState: LazyListState, totalItems: Int, modifier: Modifier = Modifier) {
    val scrollMetrics by remember(listState, totalItems) {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo.size
            val maxFirstVisibleIndex = (totalItems - visibleItems).coerceAtLeast(1)
            val progress = listState.firstVisibleItemIndex.coerceAtMost(maxFirstVisibleIndex) / maxFirstVisibleIndex.toFloat()
            visibleItems to progress
        }
    }
    val (visibleItems, progress) = scrollMetrics
    if (visibleItems == 0 || totalItems <= visibleItems) return
    BoxWithConstraints(modifier.width(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        val density = LocalDensity.current
        val viewportPx = with(density) { maxHeight.toPx() }
        val thumbPx = (viewportPx * visibleItems / totalItems.toFloat())
            .coerceAtLeast(with(density) { 32.dp.toPx() })
            .coerceAtMost(viewportPx)
        val offsetPx = ((viewportPx - thumbPx) * progress).roundToInt()
        Box(
            Modifier.offset { IntOffset(0, offsetPx) }.fillMaxWidth().height(with(density) { thumbPx.toDp() })
                .clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun BehaviorScoreCard(item: BehaviorItemEntity, selected: Int?, locked: Boolean, parentMode: Boolean, onScore: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(15.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fullScore = item.maxScore
                val partialScore = (fullScore / 2f).roundToInt().coerceIn(1, fullScore - 1)
                listOf(
                    fullScore to "😊 做到了 ${fullScore}分",
                    partialScore to "🙂 差一点 ${partialScore}分",
                    0 to "🌱 明天加油 0分",
                ).forEach { (score, label) ->
                    FilterChip(
                        selected = selected == score,
                        onClick = { onScore(if (selected == score) -1 else score) },
                        enabled = !locked,
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (parentMode && !locked) Text("家长确认分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
