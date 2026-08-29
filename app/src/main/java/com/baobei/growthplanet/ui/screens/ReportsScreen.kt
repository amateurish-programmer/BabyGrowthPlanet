package com.baobei.growthplanet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baobei.growthplanet.domain.GrowthCalculator
import com.baobei.growthplanet.settings.PlanetTheme
import com.baobei.growthplanet.ui.AppViewModel
import com.baobei.growthplanet.ui.ReportPeriod
import com.baobei.growthplanet.ui.components.PlanetHeader
import com.baobei.growthplanet.ui.components.RoundedPanel
import com.baobei.growthplanet.ui.components.ScoreProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(vm: AppViewModel, theme: PlanetTheme, modifier: Modifier = Modifier) {
    val child by vm.selectedChild.collectAsStateWithLifecycle()
    val music by vm.musicEnabled.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val range by vm.currentRange.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val balance by vm.balance.collectAsStateWithLifecycle()
    var goalText by rememberSaveable { mutableStateOf("") }
    var showGoalHelp by remember { mutableStateOf(false) }
    var editingGoalId by remember { mutableLongStateOf(0L) }
    var editingGoalTitle by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        PlanetHeader("成长报告", "每一点进步都值得被看见", child, theme, music, { vm.setMusic(!music) })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ReportPeriod.entries.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = period == item,
                            onClick = { vm.setPeriod(item) },
                            shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size),
                        ) { Text(periodLabel(item)) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton({ vm.movePeriod(-1) }) { Icon(Icons.Default.ChevronLeft, "上一周期") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(periodTitle(period, range.start), fontWeight = FontWeight.Bold)
                        Text("${range.start} 至 ${range.end}", style = MaterialTheme.typography.labelSmall)
                    }
                    Row {
                        IconButton({ vm.resetPeriod() }) { Icon(Icons.Default.Refresh, "回到当前") }
                        IconButton({ vm.movePeriod(1) }) { Icon(Icons.Default.ChevronRight, "下一周期") }
                    }
                }
            }
            item {
                RoundedPanel {
                    Text(if (report.participationDays == 0) "还没有确认记录" else "已经一起回顾 ${report.participationDays} 天", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReportStat("成长分", "${report.score}/${report.maxScore}")
                        ReportStat("星星", "${report.stars} 颗")
                        ReportStat("积分余额", balance.toString())
                    }
                    ScoreProgress(report.percent, modifier = Modifier.padding(top = 12.dp))
                    Text("总体达成 ${(report.percent * 100).toInt()}%，未确认日期不会按 0 分计算。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
            if (report.rows.isNotEmpty()) item {
                RoundedPanel {
                    Text("成长趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val values = report.rows.map { GrowthCalculator.percent(GrowthCalculator.finalScore(it.positiveScore, it.deduction), it.maxScore) }
                    TrendChart(values)
                    Text("每天的高度代表当天完成比例", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                RoundedPanel {
                    Text("六维成长地图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (report.categories.isEmpty()) Text("确认每日记录后，这里会显示各项能力的成长进度。")
                    report.categories.sortedByDescending { GrowthCalculator.percent(it.score, it.maxScore) }.forEach { row ->
                        val pct = GrowthCalculator.percent(row.score, row.maxScore)
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(row.category, Modifier.width(88.dp), style = MaterialTheme.typography.bodySmall)
                            ScoreProgress(pct, modifier = Modifier.weight(1f))
                            Text(" ${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    report.categories.maxByOrNull { GrowthCalculator.percent(it.score, it.maxScore) }?.let {
                        Text("闪光点：${it.category}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (period == ReportPeriod.WEEK) item {
                RoundedPanel {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("本周小目标（最多 10 个）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showGoalHelp = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, "什么是本周小目标")
                        }
                    }
                    goals.forEach { goal ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(goal.completed, { vm.toggleGoal(goal) })
                            if (editingGoalId == goal.id) {
                                OutlinedTextField(
                                    editingGoalTitle,
                                    { editingGoalTitle = it.take(40) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("目标内容") },
                                )
                                IconButton(
                                    onClick = { vm.updateGoal(goal, editingGoalTitle); editingGoalId = 0L },
                                    enabled = editingGoalTitle.isNotBlank(),
                                ) { Icon(Icons.Default.Check, "保存目标") }
                                IconButton(onClick = { editingGoalId = 0L }) { Icon(Icons.Default.Close, "取消编辑") }
                            } else {
                                Text(goal.title, Modifier.weight(1f))
                                IconButton(onClick = { editingGoalId = goal.id; editingGoalTitle = goal.title }) { Icon(Icons.Default.Edit, "编辑目标") }
                                IconButton(onClick = { vm.deleteGoal(goal) }) { Icon(Icons.Default.Delete, "删除目标") }
                            }
                        }
                    }
                    if (goals.size < 10) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(goalText, { goalText = it.take(40) }, placeholder = { Text("例如：每天自己整理书包") }, modifier = Modifier.weight(1f), singleLine = true)
                            Spacer(Modifier.width(8.dp))
                            Button({ vm.addGoal(goalText); goalText = "" }, enabled = goalText.isNotBlank()) { Text("添加") }
                        }
                    }
                }
            }
            item {
                RoundedPanel {
                    val badge = when {
                        report.participationDays >= 30 -> "稳定成长家"
                        report.participationDays >= 14 -> "坚持小达人"
                        report.participationDays >= 7 -> "连续探索者"
                        report.participationDays >= 3 -> "成长新芽"
                        else -> "旅程刚开始"
                    }
                    Text("坚持徽章", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(badge, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text("徽章只奖励参与和坚持，不要求每天满分。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (showGoalHelp) {
        AlertDialog(
            onDismissRequest = { showGoalHelp = false },
            title = { Text("本周小目标有什么用？") },
            text = {
                Text("每周选择 1–10 件最想重点练习的小事，例如主动整理书包、说出自己的感受或共同阅读。完成后可以勾选，方便亲子复盘和鼓励；它不增加积分、不扣分，也不影响星级。每周目标会保留在对应周报告中。")
            },
            confirmButton = { TextButton({ showGoalHelp = false }) { Text("知道了") } },
        )
    }
}

@Composable
private fun ReportStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TrendChart(values: List<Float>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(130.dp).padding(vertical = 16.dp)) {
        if (values.isEmpty()) return@Canvas
        val step = if (values.size <= 1) size.width else size.width / (values.size - 1)
        val points = values.mapIndexed { index, value -> Offset(index * step, size.height * (1f - value)) }
        points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, strokeWidth = 8f, cap = StrokeCap.Round) }
        points.forEach { drawCircle(Color.White, 9f, it); drawCircle(color, 6f, it) }
    }
}

private fun periodLabel(period: ReportPeriod) = when (period) { ReportPeriod.WEEK -> "周"; ReportPeriod.MONTH -> "月"; ReportPeriod.YEAR -> "年" }
private fun periodTitle(period: ReportPeriod, start: LocalDate) = when (period) {
    ReportPeriod.WEEK -> "${start.format(DateTimeFormatter.ofPattern("M月d日"))}这一周"
    ReportPeriod.MONTH -> start.format(DateTimeFormatter.ofPattern("yyyy年M月"))
    ReportPeriod.YEAR -> "${start.year}成长年报"
}
