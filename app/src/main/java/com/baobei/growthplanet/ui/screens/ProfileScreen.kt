package com.baobei.growthplanet.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baobei.growthplanet.data.BehaviorItemEntity
import com.baobei.growthplanet.data.ChildProfileEntity
import com.baobei.growthplanet.data.CategoryWithItems
import com.baobei.growthplanet.data.DefaultTemplate
import com.baobei.growthplanet.data.DeductionTypeEntity
import com.baobei.growthplanet.data.RewardEntity
import com.baobei.growthplanet.reminder.ReminderScheduler
import com.baobei.growthplanet.settings.PlanetTheme
import com.baobei.growthplanet.ui.AppViewModel
import com.baobei.growthplanet.ui.components.RoundedPanel
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(vm: AppViewModel, theme: PlanetTheme, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val children by vm.children.collectAsStateWithLifecycle()
    val child by vm.selectedChild.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val rewards by vm.rewards.collectAsStateWithLifecycle()
    val deductionTypes by vm.deductionTypes.collectAsStateWithLifecycle()
    val reminder by vm.reminderEnabled.collectAsStateWithLifecycle()
    val hour by vm.reminderHour.collectAsStateWithLifecycle()
    val minute by vm.reminderMinute.collectAsStateWithLifecycle()
    val versionInfo = remember(context) {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName ?: "-"}（${PackageInfoCompat.getLongVersionCode(info)}）"
    }
    var addChild by remember { mutableStateOf(false) }
    var editTemplate by remember { mutableStateOf(false) }
    var manageRewards by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ChildProfileEntity?>(null) }
    var clearTarget by remember { mutableStateOf<ChildProfileEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ChildProfileEntity?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.setReminder(true)
            ReminderScheduler.schedule(context, hour, minute)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("宝贝的成长星球", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
        item {
            RoundedPanel {
                Text("宝贝档案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                children.forEach { profile ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(child?.id == profile.id, { vm.selectChild(profile.id) })
                        Text("${profile.avatar}  ${profile.name}", Modifier.weight(1f), fontSize = 17.sp)
                        TextButton(onClick = { editingChild = profile }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("编辑") }
                        TextButton(onClick = { clearTarget = profile }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("清空") }
                        TextButton(onClick = { deleteTarget = profile }, contentPadding = PaddingValues(horizontal = 5.dp), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                    }
                }
                OutlinedButton({ addChild = true }) { Icon(Icons.Default.Add, null); Text("添加宝贝") }
            }
        }
        item {
            RoundedPanel {
                Text("主题乐园", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("每个宝贝都可以拥有自己的主题和音乐。", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PlanetTheme.SPACE to "🚀 星球", PlanetTheme.FOREST to "🦊 森林", PlanetTheme.RAINBOW to "🎈 彩虹").forEach { (item, title) ->
                        FilterChip(theme == item, { vm.setTheme(item) }, label = { Text(title) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            RoundedPanel {
                Text("每日亲子提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (reminder) "已开启 · %02d:%02d".format(hour, minute) else "默认关闭")
                        Text("非精确定时，系统可能稍后提醒", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(reminder, { enabled ->
                        if (!enabled) {
                            vm.setReminder(false)
                            ReminderScheduler.cancel(context)
                        } else if (android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            vm.setReminder(true)
                            ReminderScheduler.schedule(context, hour, minute)
                        } else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
                TextButton({
                    TimePickerDialog(context, { _, h, m ->
                        vm.setReminder(reminder, h, m)
                        if (reminder) ReminderScheduler.schedule(context, h, m)
                    }, hour, minute, true).show()
                }) { Text("设置提醒时间") }
            }
        }
        item {
            RoundedPanel {
                Text("成长模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("当前有 ${categories.sumOf { it.items.count { item -> !item.deleted } }} 个行为项目，可按成长阶段调整。")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ editTemplate = true }) { Text("管理行为项目") }
                    OutlinedButton({ manageRewards = true }) { Text("管理奖励") }
                }
            }
        }
        item {
            RoundedPanel {
                Text("亲子使用建议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                DefaultTemplate.guidance.forEach { Text("• $it", Modifier.padding(top = 8.dp)) }
            }
        }
        item {
            RoundedPanel {
                Text("隐私与数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("无账号、无广告、无分析 SDK，不申请网络权限。记录保存在本机，并可由你自己的 Android 系统备份恢复。", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            RoundedPanel {
                Text("版本信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("宝贝成长星球", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
                Text("版本 $versionInfo", style = MaterialTheme.typography.bodyMedium)
                Text("离线版 · com.baobei.growthplanet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (addChild) AddChildDialog({ addChild = false }, { name, avatar -> vm.createChild(name, avatar); addChild = false })
    if (manageRewards) ManageRewardsDialog(rewards, vm, { manageRewards = false })
    if (editTemplate) TemplateDialog(categories, deductionTypes, vm, { editTemplate = false })
    editingChild?.let { profile ->
        EditChildDialog(profile, { editingChild = null }) { name, avatar ->
            vm.updateChild(profile, name, avatar)
            editingChild = null
        }
    }
    clearTarget?.let { profile ->
        ConfirmChildActionDialog(
            title = "清空 ${profile.name} 的成长记录？",
            message = "每日打分、周目标和奖励兑换记录都会删除；宝贝档案、行为项目和奖励设置会保留。此操作无法撤销。",
            confirmText = "确认清空",
            onDismiss = { clearTarget = null },
            onConfirm = { vm.clearChildRecords(profile); clearTarget = null },
        )
    }
    deleteTarget?.let { profile ->
        ConfirmChildActionDialog(
            title = "删除宝贝档案？",
            message = "将永久删除 ${profile.name} 的档案、全部成长记录、行为模板和奖励信息。此操作无法撤销。",
            confirmText = "确认删除",
            onDismiss = { deleteTarget = null },
            onConfirm = { vm.deleteChild(profile); deleteTarget = null },
        )
    }
}

@Composable
private fun EditChildDialog(profile: ChildProfileEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var avatar by remember(profile.id) { mutableStateOf(profile.avatar) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button({ onSave(name, avatar) }, enabled = name.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
        title = { Text("编辑宝贝档案") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(12) }, label = { Text("小名") }, singleLine = true)
                Row { listOf("🚀", "🦊", "🎈", "🐼", "🐰").forEach { FilterChip(avatar == it, { avatar = it }, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp)) } }
            }
        },
    )
}

@Composable
private fun ConfirmChildActionDialog(title: String, message: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(confirmText) } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AddChildDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("🚀") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button({ onAdd(name, avatar) }, enabled = name.isNotBlank()) { Text("创建") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
        title = { Text("添加宝贝档案") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(12) }, label = { Text("小名") })
                Row { listOf("🚀", "🦊", "🎈", "🐼", "🐰").forEach { FilterChip(avatar == it, { avatar = it }, label = { Text(it) }, modifier = Modifier.padding(end = 4.dp)) } }
            }
        },
    )
}

@Composable
private fun ManageRewardsDialog(rewards: List<RewardEntity>, vm: AppViewModel, onDismiss: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RewardEntity?>(null) }
    var editingId by remember { mutableLongStateOf(0L) }
    var editingTitle by remember { mutableStateOf("") }
    var editingCost by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理奖励") },
        confirmButton = { TextButton(onDismiss) { Text("完成") } },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 540.dp)) {
                if (rewards.isEmpty()) {
                    Text("还没有奖励，可以先添加一个。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        rewards.forEach { reward ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(reward.emoji, fontSize = 28.sp)
                                if (editingId == reward.id) {
                                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                        OutlinedTextField(
                                            editingTitle,
                                            { editingTitle = it.take(40) },
                                            label = { Text("奖励内容") },
                                            singleLine = true,
                                        )
                                        OutlinedTextField(
                                            editingCost,
                                            { editingCost = it.filter(Char::isDigit).take(4) },
                                            label = { Text("所需积分") },
                                            singleLine = true,
                                        )
                                    }
                                    Column {
                                        IconButton(
                                            onClick = { vm.editReward(reward, editingTitle, editingCost.toIntOrNull() ?: 0); editingId = 0L },
                                            enabled = editingTitle.isNotBlank() && (editingCost.toIntOrNull() ?: 0) > 0,
                                        ) { Icon(Icons.Default.Check, "保存奖励") }
                                        IconButton(onClick = { editingId = 0L }) { Icon(Icons.Default.Close, "取消修改") }
                                    }
                                } else {
                                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                        Text(reward.title, fontWeight = FontWeight.SemiBold)
                                        Text("${reward.cost} 积分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        editingId = reward.id
                                        editingTitle = reward.title
                                        editingCost = reward.cost.toString()
                                    }) { Icon(Icons.Default.Edit, "编辑 ${reward.title}") }
                                    IconButton(onClick = { deleteTarget = reward }) { Icon(Icons.Default.Delete, "删除 ${reward.title}") }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("添加奖励")
                }
            }
        },
    )
    if (showAdd) AddRewardDialog(
        onDismiss = { showAdd = false },
        onAdd = { title, emoji, cost -> vm.addReward(title, emoji, cost); showAdd = false },
    )
    deleteTarget?.let { reward ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除奖励？") },
            text = { Text("确认删除“${reward.title}”？已有兑换记录会保留原奖励名称和积分。") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteReward(reward); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("取消") } },
        )
    }
}
@Composable
private fun AddRewardDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("50") }
    var emoji by remember { mutableStateOf("🎁") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button({ onAdd(title, emoji.ifBlank { "🎁" }, cost.toIntOrNull() ?: 0) }, enabled = title.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0) { Text("添加") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
        title = { Text("添加家庭奖励") },
        text = {
            Column {
                OutlinedTextField(title, { title = it.take(40) }, label = { Text("奖励内容") }, singleLine = true)
                OutlinedTextField(cost, { cost = it.filter(Char::isDigit).take(4) }, label = { Text("所需积分") }, singleLine = true)
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(12) },
                    label = { Text("图标（支持输入法 Emoji 表情）") },
                    supportingText = { Text("例如：🎁、🎡、📚") },
                    singleLine = true,
                )
            }
        },
    )
}@Composable
private fun TemplateDialog(
    categories: List<CategoryWithItems>,
    deductions: List<DeductionTypeEntity>,
    vm: AppViewModel,
    onDismiss: () -> Unit,
) {
    var selectedCategory by remember(categories) { mutableLongStateOf(categories.firstOrNull()?.category?.id ?: 0L) }
    var newTitle by remember { mutableStateOf("") }
    var newScore by remember { mutableStateOf("2") }
    var categoryMenu by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableLongStateOf(0L) }
    var editingItemTitle by remember { mutableStateOf("") }
    var editingItemScore by remember { mutableStateOf("2") }
    var editingDeductionId by remember { mutableLongStateOf(0L) }
    var editingDeductionTitle by remember { mutableStateOf("") }
    var editingDeductionPoints by remember { mutableStateOf("2") }
    var deleteDeductionTarget by remember { mutableStateOf<DeductionTypeEntity?>(null) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val score = newScore.toIntOrNull() ?: 0
            val canAdd = newTitle.isNotBlank() && if (selectedCategory == -1L) score in 1..99 else selectedCategory > 0 && score in 2..20
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (selectedCategory == -1L) vm.addDeductionType(newTitle, score)
                        else vm.addItem(selectedCategory, newTitle, score)
                        newTitle = ""
                        newScore = "2"
                    },
                    enabled = canAdd,
                ) { Text("添加") }
                TextButton(onDismiss) { Text("完成") }
            }
        },
        title = { Text("管理行为与扣分") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 580.dp)) {
                Text("可编辑行为名称、满分及小插曲扣分规则。历史记录仍使用当时的名称和分值。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scrollState).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        categories.forEach { group ->
                            Text("${group.category.emoji} ${group.category.title}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                            group.items.filter { !it.deleted }.forEach { item ->
                                BehaviorItemRow(
                                    item = item,
                                    isEditing = editingItemId == item.id,
                                    editingTitle = editingItemTitle,
                                    editingScore = editingItemScore,
                                    onEditingTitleChange = { editingItemTitle = it.take(30) },
                                    onEditingScoreChange = { editingItemScore = it.filter(Char::isDigit).take(2) },
                                    onToggle = { vm.toggleItem(item) },
                                    onEdit = {
                                        editingItemId = item.id
                                        editingItemTitle = item.title
                                        editingItemScore = item.maxScore.toString()
                                    },
                                    onSave = {
                                        vm.updateItem(item, editingItemTitle, editingItemScore.toIntOrNull() ?: 0)
                                        editingItemId = 0L
                                    },
                                    onCancel = { editingItemId = 0L },
                                    onDelete = { vm.deleteItem(item) },
                                )
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text("需要复盘的小插曲（扣分项）", fontWeight = FontWeight.Bold)
                        deductions.forEach { type ->
                            DeductionTypeRow(
                                type = type,
                                isEditing = editingDeductionId == type.id,
                                editingTitle = editingDeductionTitle,
                                editingPoints = editingDeductionPoints,
                                onEditingTitleChange = { editingDeductionTitle = it.take(40) },
                                onEditingPointsChange = { editingDeductionPoints = it.filter(Char::isDigit).take(2) },
                                onEdit = {
                                    editingDeductionId = type.id
                                    editingDeductionTitle = type.title
                                    editingDeductionPoints = type.points.toString()
                                },
                                onSave = {
                                    vm.updateDeductionType(type, editingDeductionTitle, editingDeductionPoints.toIntOrNull() ?: 0)
                                    editingDeductionId = 0L
                                },
                                onCancel = { editingDeductionId = 0L },
                                onDelete = { deleteDeductionTarget = type },
                            )
                        }
                    }
                    TemplateScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Text("添加行为或扣分项", fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (selectedCategory == -1L) "⚠️ 扣分项"
                            else categories.firstOrNull { it.category.id == selectedCategory }?.let { "${it.category.emoji} ${it.category.title}" } ?: "选择类型",
                        )
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("⚠️ 扣分项") },
                            onClick = { selectedCategory = -1L; categoryMenu = false },
                        )
                        categories.forEach { group ->
                            DropdownMenuItem(
                                text = { Text("${group.category.emoji} ${group.category.title}") },
                                onClick = { selectedCategory = group.category.id; categoryMenu = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(40) },
                    label = { Text(if (selectedCategory == -1L) "扣分项内容" else "行为项目名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = newScore,
                    onValueChange = { newScore = it.filter(Char::isDigit).take(2) },
                    label = { Text(if (selectedCategory == -1L) "每次扣分（1–99）" else "项目满分（2–20）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
    )

    deleteDeductionTarget?.let { type ->
        AlertDialog(
            onDismissRequest = { deleteDeductionTarget = null },
            title = { Text("删除扣分项？") },
            text = { Text("删除“${type.title}”后将不能再添加该小插曲，已有历史记录不会改变。") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteDeductionType(type); deleteDeductionTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("删除") }
            },
            dismissButton = { TextButton({ deleteDeductionTarget = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun BehaviorItemRow(
    item: BehaviorItemEntity,
    isEditing: Boolean,
    editingTitle: String,
    editingScore: String,
    onEditingTitleChange: (String) -> Unit,
    onEditingScoreChange: (String) -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(item.enabled, onCheckedChange = { onToggle() })
        if (isEditing) {
            Column(Modifier.weight(1f)) {
                OutlinedTextField(editingTitle, onEditingTitleChange, label = { Text("行为名称") }, singleLine = true)
                OutlinedTextField(
                    editingScore,
                    onEditingScoreChange,
                    label = { Text("满分（2–20）") },
                    singleLine = true,
                    modifier = Modifier.width(130.dp),
                )
            }
            Column {
                IconButton(onClick = onSave, enabled = editingTitle.isNotBlank() && (editingScore.toIntOrNull() ?: 0) in 2..20) { Icon(Icons.Default.Check, "保存修改") }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "取消修改") }
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(item.title, color = if (item.enabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("满分 ${item.maxScore} 分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑 ${item.title}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除 ${item.title}") }
        }
    }
}

@Composable
private fun DeductionTypeRow(
    type: DeductionTypeEntity,
    isEditing: Boolean,
    editingTitle: String,
    editingPoints: String,
    onEditingTitleChange: (String) -> Unit,
    onEditingPointsChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        if (isEditing) {
            Column(Modifier.weight(1f)) {
                OutlinedTextField(editingTitle, onEditingTitleChange, label = { Text("扣分项内容") }, singleLine = true)
                OutlinedTextField(
                    editingPoints,
                    onEditingPointsChange,
                    label = { Text("扣分（1–99）") },
                    singleLine = true,
                    modifier = Modifier.width(130.dp),
                )
            }
            Column {
                IconButton(onClick = onSave, enabled = editingTitle.isNotBlank() && (editingPoints.toIntOrNull() ?: 0) in 1..99) { Icon(Icons.Default.Check, "保存扣分项") }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "取消修改") }
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(type.title)
                Text("每次扣 ${type.points} 分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑 ${type.title}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除 ${type.title}") }
        }
    }
}
@Composable
private fun TemplateScrollIndicator(scrollState: androidx.compose.foundation.ScrollState, modifier: Modifier = Modifier) {
    if (scrollState.maxValue <= 0) return
    BoxWithConstraints(
        modifier.width(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val density = LocalDensity.current
        val viewportPx = with(density) { maxHeight.toPx() }
        val totalPx = viewportPx + scrollState.maxValue
        val minThumbPx = with(density) { 36.dp.toPx() }
        val thumbPx = (viewportPx * viewportPx / totalPx).coerceAtLeast(minThumbPx).coerceAtMost(viewportPx)
        val progress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        val offsetPx = ((viewportPx - thumbPx) * progress).roundToInt()
        Box(
            Modifier
                .offset { IntOffset(0, offsetPx) }
                .fillMaxWidth()
                .height(with(density) { thumbPx.toDp() })
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
