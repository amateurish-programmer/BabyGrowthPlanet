package com.baobei.growthplanet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baobei.growthplanet.data.*
import com.baobei.growthplanet.domain.DateRange
import com.baobei.growthplanet.domain.DateRanges
import com.baobei.growthplanet.settings.AppSettings
import com.baobei.growthplanet.settings.PlanetTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ReportPeriod { WEEK, MONTH, YEAR }

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel(private val repository: GrowthRepository, private val settings: AppSettings) : ViewModel() {
    val children = repository.children.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedChildId = settings.selectedChild
    val selectedChild = combine(children, selectedChildId) { list, id -> list.firstOrNull { it.id == id } ?: list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val musicEnabled = settings.musicEnabled
    val reminderEnabled = settings.reminderEnabled
    val reminderHour = settings.reminderHour
    val reminderMinute = settings.reminderMinute

    val categories = selectedChild.filterNotNull().flatMapLatest { repository.categories(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deductionTypes = selectedChild.filterNotNull().flatMapLatest { repository.deductionTypes(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rewards = selectedChild.filterNotNull().flatMapLatest { repository.rewards(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val redemptions = selectedChild.filterNotNull().flatMapLatest { repository.redemptions(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val balance = selectedChild.filterNotNull().flatMapLatest { repository.balance(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _today = MutableStateFlow<TodayData?>(null)
    val today = _today.asStateFlow()
    private val _todayError = MutableStateFlow<String?>(null)
    val todayError = _todayError.asStateFlow()
    private val _period = MutableStateFlow(ReportPeriod.WEEK)
    val period = _period.asStateFlow()
    private val _anchorDate = MutableStateFlow(LocalDate.now())
    val anchorDate = _anchorDate.asStateFlow()
    private val reportRange: Flow<DateRange> = combine(_period, _anchorDate) { period, date ->
        when (period) { ReportPeriod.WEEK -> DateRanges.week(date); ReportPeriod.MONTH -> DateRanges.month(date); ReportPeriod.YEAR -> DateRanges.year(date) }
    }
    val report = combine(selectedChild.filterNotNull(), reportRange) { child, range -> child.id to range }
        .flatMapLatest { (id, range) -> repository.report(id, range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportData())
    val currentRange = reportRange.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DateRanges.week(LocalDate.now()))
    val goals = combine(selectedChild.filterNotNull(), _anchorDate) { child, date -> child.id to DateRanges.week(date).startText }
        .flatMapLatest { (id, week) -> repository.goals(id, week) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { children.filter { it.isNotEmpty() }.collect { list -> if (selectedChildId.value !in list.map { it.id }) selectChild(list.first().id) } }
        viewModelScope.launch {
            selectedChild.filterNotNull().collect { child ->
                repository.ensureDefaultDeductionTypes(child.id)
                loadToday()
            }
        }
    }

    fun createChild(name: String, avatar: String) = viewModelScope.launch { selectChild(repository.createChild(name, avatar)) }
    fun updateChild(child: ChildProfileEntity, name: String, avatar: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.updateChild(child.copy(name = name.trim(), avatar = avatar))
    }
    fun clearChildRecords(child: ChildProfileEntity) = viewModelScope.launch {
        repository.clearChildRecords(child.id)
        if (selectedChild.value?.id == child.id) loadToday()
    }
    fun deleteChild(child: ChildProfileEntity) = viewModelScope.launch {
        val replacementId = children.value.firstOrNull { it.id != child.id }?.id ?: 0L
        repository.deleteChild(child.id)
        settings.setSelectedChild(replacementId)
        _today.value = null
    }
    fun selectChild(id: Long) { settings.setSelectedChild(id) }
    fun setMusic(value: Boolean) = settings.setMusicEnabled(value)
    fun setTheme(theme: PlanetTheme) = viewModelScope.launch { selectedChild.value?.let { repository.updateChild(it.copy(theme = theme.name, avatar = when(theme) { PlanetTheme.SPACE -> "🚀"; PlanetTheme.FOREST -> "🦊"; PlanetTheme.RAINBOW -> "🎈" })) } }
    fun setReminder(enabled: Boolean, hour: Int = reminderHour.value, minute: Int = reminderMinute.value) = settings.setReminder(enabled, hour, minute)

    fun loadToday() = viewModelScope.launch {
        val child = selectedChild.value ?: return@launch
        _todayError.value = null
        runCatching { repository.today(child.id) }
            .onSuccess { _today.value = it }
            .onFailure { _todayError.value = it.message ?: "今日数据读取失败" }
    }
    fun score(itemId: Long, value: Int, parent: Boolean = false) = viewModelScope.launch { _today.value?.let { repository.setScore(it.record.id, itemId, value, parent); loadToday() } }
    fun confirm(proud: String, encouragement: String) = viewModelScope.launch { _today.value?.let { repository.confirmRecord(it.record.id, proud, encouragement); loadToday() } }
    fun resetToday() = viewModelScope.launch {
        _today.value?.let { repository.resetToday(it.record.id) }
        loadToday()
    }
    fun addDeduction(type: DeductionTypeEntity) = viewModelScope.launch { _today.value?.let { repository.addDeduction(it.record.id, type); loadToday() } }
    fun removeDeduction(id: Long) = viewModelScope.launch { repository.deleteDeduction(id); loadToday() }
    fun updateDeductionType(type: DeductionTypeEntity, title: String, points: Int) = viewModelScope.launch {
        if (title.isNotBlank() && points > 0) repository.updateDeductionType(type.copy(title = title.trim(), points = points))
    }
    fun deleteDeductionType(type: DeductionTypeEntity) = viewModelScope.launch {
        repository.updateDeductionType(type.copy(enabled = false))
    }

    fun setPeriod(value: ReportPeriod) { _period.value = value }
    fun movePeriod(delta: Long) { _anchorDate.value = when (_period.value) { ReportPeriod.WEEK -> _anchorDate.value.plusWeeks(delta); ReportPeriod.MONTH -> _anchorDate.value.plusMonths(delta); ReportPeriod.YEAR -> _anchorDate.value.plusYears(delta) } }
    fun resetPeriod() { _anchorDate.value = LocalDate.now() }
    fun addGoal(title: String) = viewModelScope.launch { val child = selectedChild.value ?: return@launch; if (goals.value.size < 10) repository.addGoal(child.id, DateRanges.week(_anchorDate.value).startText, title) }
    fun toggleGoal(goal: WeeklyGoalEntity) = viewModelScope.launch { repository.toggleGoal(goal) }
    fun updateGoal(goal: WeeklyGoalEntity, title: String) = viewModelScope.launch {
        if (title.isNotBlank()) repository.updateGoal(goal.copy(title = title.trim()))
    }
    fun deleteGoal(goal: WeeklyGoalEntity) = viewModelScope.launch { repository.deleteGoal(goal.id) }

    fun redeem(reward: RewardEntity, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(repository.redeem(reward.childId, reward)) }
    fun addReward(title: String, emoji: String, cost: Int) = viewModelScope.launch { selectedChild.value?.let { repository.addReward(it.id, title, emoji, cost) } }
    fun toggleReward(reward: RewardEntity) = viewModelScope.launch { repository.updateReward(reward.copy(enabled = !reward.enabled)) }
    fun deleteReward(reward: RewardEntity) = viewModelScope.launch { repository.deleteReward(reward.id) }
    fun editReward(reward: RewardEntity, title: String, cost: Int) = viewModelScope.launch {
        if (title.isNotBlank() && cost > 0) repository.updateReward(reward.copy(title = title.trim(), cost = cost))
    }
    fun toggleItem(item: BehaviorItemEntity) = viewModelScope.launch { repository.updateItem(item.copy(enabled = !item.enabled)); loadToday() }
    fun deleteItem(item: BehaviorItemEntity) = viewModelScope.launch { repository.updateItem(item.copy(deleted = true, enabled = false)); loadToday() }
    fun updateItem(item: BehaviorItemEntity, title: String, maxScore: Int) = viewModelScope.launch {
        if (title.isNotBlank() && maxScore in 2..20) {
            repository.updateItem(item.copy(title = title.trim(), maxScore = maxScore))
            loadToday()
        }
    }
    fun addItem(categoryId: Long, title: String, maxScore: Int) = viewModelScope.launch {
        selectedChild.value?.let { repository.addItem(it.id, categoryId, title, maxScore); loadToday() }
    }
    fun addDeductionType(title: String, points: Int) = viewModelScope.launch {
        selectedChild.value?.let { repository.addDeductionType(it.id, title, points) }
    }

    class Factory(private val repository: GrowthRepository, private val settings: AppSettings) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository, settings) as T
    }
}
