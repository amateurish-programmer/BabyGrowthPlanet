package com.baobei.growthplanet.data

import androidx.room.withTransaction
import com.baobei.growthplanet.domain.DateRange
import com.baobei.growthplanet.domain.GrowthCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class TodayData(
    val record: DailyRecordEntity,
    val categories: List<CategoryWithItems>,
    val scores: Map<Long, BehaviorScoreEntity>,
    val deductions: List<DeductionEntryEntity>,
)

data class ReportData(
    val rows: List<RecordSummaryRow> = emptyList(),
    val categories: List<CategoryScoreRow> = emptyList(),
) {
    val score = GrowthCalculator.totalScore(rows)
    val maxScore = GrowthCalculator.totalMax(rows)
    val stars = GrowthCalculator.totalStars(rows)
    val participationDays = rows.size
    val percent = GrowthCalculator.percent(score, maxScore)
}

class GrowthRepository(private val database: GrowthDatabase) {
    private val dao = database.dao()
    val children = dao.observeChildren()

    suspend fun createChild(name: String, avatar: String = "🚀"): Long = database.withTransaction {
        val childId = dao.insertChild(ChildProfileEntity(name = name.trim().ifBlank { "小星星" }, avatar = avatar))
        var itemOrder = 0
        DefaultTemplate.categories.forEachIndexed { categoryIndex, template ->
            val categoryId = dao.insertCategory(
                BehaviorCategoryEntity(childId = childId, title = template.title, emoji = template.emoji, sortOrder = categoryIndex)
            )
            template.items.forEach { title ->
                dao.insertItem(BehaviorItemEntity(childId = childId, categoryId = categoryId, title = title, sortOrder = itemOrder++))
            }
        }
        DefaultTemplate.deductions.forEach { (title, points) ->
            dao.insertDeductionType(DeductionTypeEntity(childId = childId, title = title, points = points))
        }
        DefaultTemplate.rewards.forEachIndexed { index, (title, emoji, cost) ->
            dao.insertReward(RewardEntity(childId = childId, title = title, emoji = emoji, cost = cost, sortOrder = index))
        }
        childId
    }

    suspend fun updateChild(child: ChildProfileEntity) = dao.updateChild(child)

    suspend fun resetToday(recordId: Long) = dao.deleteRecord(recordId)

    suspend fun clearChildRecords(childId: Long) = database.withTransaction {
        dao.deleteRecordsForChild(childId)
        dao.deleteGoalsForChild(childId)
        dao.deleteRedemptionsForChild(childId)
    }

    suspend fun deleteChild(childId: Long) = database.withTransaction {
        dao.deleteRecordsForChild(childId)
        dao.deleteGoalsForChild(childId)
        dao.deleteRedemptionsForChild(childId)
        dao.deleteRewardsForChild(childId)
        dao.deleteChild(childId)
    }
    fun categories(childId: Long) = dao.observeCategories(childId)
    fun deductionTypes(childId: Long) = dao.observeDeductionTypes(childId)
    fun rewards(childId: Long) = dao.observeRewards(childId)
    fun redemptions(childId: Long) = dao.observeRedemptions(childId)

    suspend fun ensureDefaultDeductionTypes(childId: Long) = database.withTransaction {
        val existingTitles = dao.allDeductionTypes(childId).mapTo(mutableSetOf()) { it.title }
        DefaultTemplate.deductions.forEach { (title, points) ->
            if (title !in existingTitles) {
                dao.insertDeductionType(DeductionTypeEntity(childId = childId, title = title, points = points))
            }
        }
    }

    suspend fun today(childId: Long, date: LocalDate = LocalDate.now()): TodayData = database.withTransaction {
        val dateText = date.toString()
        var record = dao.record(childId, dateText)
        if (record == null) {
            val recordId = dao.insertRecord(DailyRecordEntity(childId = childId, date = dateText))
            record = dao.recordById(recordId)!!
        }
        val categoryList = dao.categoriesNow(childId)
        val existingScores = dao.scores(record.id).associateBy { it.itemId }.toMutableMap()
        val activeCategoryIds = dao.activeCategories(childId).map { it.id }.toSet()
        dao.activeItems(childId).filter { it.categoryId in activeCategoryIds }.forEach { item ->
            if (item.id !in existingScores) {
                val category = categoryList.first { it.category.id == item.categoryId }.category
                val score = BehaviorScoreEntity(
                    recordId = record.id,
                    itemId = item.id,
                    itemTitleSnapshot = item.title,
                    categoryTitleSnapshot = category.title,
                    maxScoreSnapshot = item.maxScore,
                )
                val id = dao.upsertScore(score)
                existingScores[item.id] = score.copy(id = id)
            }
        }
        TodayData(record, categoryList, existingScores, dao.deductions(record.id))
    }

    suspend fun setScore(recordId: Long, itemId: Long, value: Int, parent: Boolean) {
        val score = dao.score(recordId, itemId) ?: return
        val normalized = if (value == -1) -1 else value.coerceIn(0, score.maxScoreSnapshot)
        dao.updateScore(if (parent) score.copy(parentScore = normalized) else score.copy(selfScore = normalized))
    }

    suspend fun confirmRecord(recordId: Long, proud: String, encouragement: String) = database.withTransaction {
        val record = dao.recordById(recordId) ?: return@withTransaction
        dao.scores(recordId).forEach { score ->
            val fallback = if (score.selfScore >= 0) score.selfScore else 0
            if (score.parentScore < 0) dao.updateScore(score.copy(parentScore = fallback))
        }
        dao.updateRecord(record.copy(confirmed = true, proudMoment = proud, encouragement = encouragement, confirmedAt = System.currentTimeMillis()))
    }

    suspend fun addDeduction(recordId: Long, type: DeductionTypeEntity, note: String = "") =
        dao.insertDeduction(DeductionEntryEntity(recordId = recordId, typeId = type.id, titleSnapshot = type.title, pointsSnapshot = type.points, note = note))

    suspend fun deleteDeduction(id: Long) = dao.deleteDeduction(id)
    suspend fun updateDeductionType(type: DeductionTypeEntity) = dao.updateDeductionType(type)
    suspend fun addDeductionType(childId: Long, title: String, points: Int) {
        if (title.isNotBlank() && points in 1..99) {
            dao.insertDeductionType(DeductionTypeEntity(childId = childId, title = title.trim(), points = points, custom = true))
        }
    }
    suspend fun addGoal(childId: Long, weekStart: String, title: String) {
        if (title.isNotBlank()) dao.insertGoal(WeeklyGoalEntity(childId = childId, weekStart = weekStart, title = title.trim()))
    }
    fun goals(childId: Long, weekStart: String) = dao.observeGoals(childId, weekStart)
    suspend fun toggleGoal(goal: WeeklyGoalEntity) = dao.updateGoal(goal.copy(completed = !goal.completed))
    suspend fun updateGoal(goal: WeeklyGoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goalId: Long) = dao.deleteGoal(goalId)

    fun report(childId: Long, range: DateRange): Flow<ReportData> = combine(
        dao.observeRecordSummaries(childId, range.startText, range.endText),
        dao.observeCategoryScores(childId, range.startText, range.endText),
    ) { rows, categories -> ReportData(rows, categories) }

    fun balance(childId: Long): Flow<Int> = combine(dao.observeEarnedPoints(childId), dao.observeSpentPoints(childId)) { earned, spent -> earned - spent }

    suspend fun redeem(childId: Long, reward: RewardEntity): Boolean {
        val balance = balance(childId).first()
        if (balance < reward.cost) return false
        dao.insertRedemption(RewardRedemptionEntity(childId = childId, rewardId = reward.id, rewardTitleSnapshot = reward.title, costSnapshot = reward.cost))
        return true
    }

    suspend fun addReward(childId: Long, title: String, emoji: String, cost: Int) {
        if (title.isNotBlank() && cost > 0) dao.insertReward(RewardEntity(childId = childId, title = title.trim(), emoji = emoji, cost = cost, sortOrder = 999))
    }

    suspend fun updateReward(reward: RewardEntity) = dao.updateReward(reward)
    suspend fun deleteReward(rewardId: Long) = dao.deleteReward(rewardId)
    suspend fun updateItem(item: BehaviorItemEntity) = dao.updateItem(item)
    suspend fun updateCategory(category: BehaviorCategoryEntity) = dao.updateCategory(category)

    suspend fun addItem(childId: Long, categoryId: Long, title: String, maxScore: Int) {
        if (title.isBlank() || maxScore !in 2..20) return
        val order = dao.activeItems(childId).maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        dao.insertItem(BehaviorItemEntity(childId = childId, categoryId = categoryId, title = title.trim(), sortOrder = order, maxScore = maxScore))
    }
}
