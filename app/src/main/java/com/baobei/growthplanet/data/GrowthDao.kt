package com.baobei.growthplanet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Query("SELECT * FROM children WHERE archived = 0 ORDER BY createdAt")
    fun observeChildren(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM children WHERE id = :id")
    suspend fun child(id: Long): ChildProfileEntity?

    @Insert suspend fun insertChild(child: ChildProfileEntity): Long
    @Update suspend fun updateChild(child: ChildProfileEntity)
    @Query("DELETE FROM children WHERE id = :childId")
    suspend fun deleteChild(childId: Long)

    @Insert suspend fun insertCategory(category: BehaviorCategoryEntity): Long
    @Insert suspend fun insertItem(item: BehaviorItemEntity): Long
    @Update suspend fun updateCategory(category: BehaviorCategoryEntity)
    @Update suspend fun updateItem(item: BehaviorItemEntity)

    @Transaction
    @Query("SELECT * FROM behavior_categories WHERE childId = :childId AND deleted = 0 ORDER BY sortOrder")
    fun observeCategories(childId: Long): Flow<List<CategoryWithItems>>

    @Transaction
    @Query("SELECT * FROM behavior_categories WHERE childId = :childId AND deleted = 0 ORDER BY sortOrder")
    suspend fun categoriesNow(childId: Long): List<CategoryWithItems>

    @Query("SELECT * FROM behavior_categories WHERE childId = :childId AND enabled = 1 AND deleted = 0 ORDER BY sortOrder")
    suspend fun activeCategories(childId: Long): List<BehaviorCategoryEntity>

    @Query("SELECT * FROM behavior_items WHERE childId = :childId AND enabled = 1 AND deleted = 0 ORDER BY sortOrder")
    suspend fun activeItems(childId: Long): List<BehaviorItemEntity>

    @Query("SELECT * FROM behavior_items WHERE id = :id")
    suspend fun item(id: Long): BehaviorItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRecord(record: DailyRecordEntity): Long
    @Update suspend fun updateRecord(record: DailyRecordEntity)
    @Query("SELECT * FROM daily_records WHERE childId = :childId AND date = :date LIMIT 1")
    suspend fun record(childId: Long, date: String): DailyRecordEntity?
    @Query("SELECT * FROM daily_records WHERE id = :id")
    suspend fun recordById(id: Long): DailyRecordEntity?
    @Query("DELETE FROM daily_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: Long)
    @Query("DELETE FROM daily_records WHERE childId = :childId")
    suspend fun deleteRecordsForChild(childId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertScore(score: BehaviorScoreEntity): Long
    @Update suspend fun updateScore(score: BehaviorScoreEntity)
    @Query("SELECT * FROM behavior_scores WHERE recordId = :recordId ORDER BY id")
    suspend fun scores(recordId: Long): List<BehaviorScoreEntity>
    @Query("SELECT * FROM behavior_scores WHERE recordId = :recordId AND itemId = :itemId LIMIT 1")
    suspend fun score(recordId: Long, itemId: Long): BehaviorScoreEntity?

    @Insert suspend fun insertDeductionType(type: DeductionTypeEntity): Long
    @Update suspend fun updateDeductionType(type: DeductionTypeEntity)
    @Query("SELECT * FROM deduction_types WHERE childId = :childId ORDER BY id")
    suspend fun allDeductionTypes(childId: Long): List<DeductionTypeEntity>
    @Query("SELECT * FROM deduction_types WHERE childId = :childId AND enabled = 1 ORDER BY id")
    fun observeDeductionTypes(childId: Long): Flow<List<DeductionTypeEntity>>
    @Insert suspend fun insertDeduction(entry: DeductionEntryEntity): Long
    @Query("DELETE FROM deduction_entries WHERE id = :id") suspend fun deleteDeduction(id: Long)
    @Query("SELECT * FROM deduction_entries WHERE recordId = :recordId ORDER BY id DESC")
    suspend fun deductions(recordId: Long): List<DeductionEntryEntity>

    @Query("DELETE FROM weekly_goals WHERE childId = :childId")
    suspend fun deleteGoalsForChild(childId: Long)
    @Insert suspend fun insertGoal(goal: WeeklyGoalEntity): Long
    @Update suspend fun updateGoal(goal: WeeklyGoalEntity)
    @Query("DELETE FROM weekly_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)
    @Query("SELECT * FROM weekly_goals WHERE childId = :childId AND weekStart = :weekStart ORDER BY id")
    fun observeGoals(childId: Long, weekStart: String): Flow<List<WeeklyGoalEntity>>

    @Query("DELETE FROM reward_redemptions WHERE childId = :childId")
    suspend fun deleteRedemptionsForChild(childId: Long)
    @Query("DELETE FROM rewards WHERE childId = :childId")
    suspend fun deleteRewardsForChild(childId: Long)
    @Insert suspend fun insertReward(reward: RewardEntity): Long
    @Update suspend fun updateReward(reward: RewardEntity)
    @Query("DELETE FROM rewards WHERE id = :rewardId")
    suspend fun deleteReward(rewardId: Long)
    @Query("SELECT * FROM rewards WHERE childId = :childId AND enabled = 1 ORDER BY cost, sortOrder")
    fun observeRewards(childId: Long): Flow<List<RewardEntity>>
    @Insert suspend fun insertRedemption(redemption: RewardRedemptionEntity): Long
    @Query("SELECT * FROM reward_redemptions WHERE childId = :childId ORDER BY redeemedAt DESC")
    fun observeRedemptions(childId: Long): Flow<List<RewardRedemptionEntity>>

    @Query("""
        SELECT r.id AS recordId, r.date AS date,
          COALESCE(SUM(CASE WHEN s.parentScore >= 0 THEN s.parentScore ELSE s.selfScore END), 0) AS positiveScore,
          COALESCE(SUM(s.maxScoreSnapshot), 0) AS maxScore,
          COALESCE((SELECT SUM(d.pointsSnapshot * d.count) FROM deduction_entries d WHERE d.recordId = r.id), 0) AS deduction
        FROM daily_records r LEFT JOIN behavior_scores s ON s.recordId = r.id
        WHERE r.childId = :childId AND r.confirmed = 1 AND r.date BETWEEN :start AND :end
        GROUP BY r.id ORDER BY r.date
    """)
    fun observeRecordSummaries(childId: Long, start: String, end: String): Flow<List<RecordSummaryRow>>

    @Query("""
        SELECT s.categoryTitleSnapshot AS category,
          COALESCE(SUM(CASE WHEN s.parentScore >= 0 THEN s.parentScore ELSE s.selfScore END), 0) AS score,
          COALESCE(SUM(s.maxScoreSnapshot), 0) AS maxScore
        FROM behavior_scores s JOIN daily_records r ON r.id = s.recordId
        WHERE r.childId = :childId AND r.confirmed = 1 AND r.date BETWEEN :start AND :end
        GROUP BY s.categoryTitleSnapshot ORDER BY s.categoryTitleSnapshot
    """)
    fun observeCategoryScores(childId: Long, start: String, end: String): Flow<List<CategoryScoreRow>>

    @Query("SELECT COALESCE(SUM(CASE WHEN positive - deduction < 0 THEN 0 ELSE positive - deduction END), 0) FROM (SELECT r.id, COALESCE(SUM(CASE WHEN s.parentScore >= 0 THEN s.parentScore ELSE s.selfScore END),0) positive, COALESCE((SELECT SUM(d.pointsSnapshot*d.count) FROM deduction_entries d WHERE d.recordId=r.id),0) deduction FROM daily_records r LEFT JOIN behavior_scores s ON s.recordId=r.id WHERE r.childId=:childId AND r.confirmed=1 GROUP BY r.id)")
    fun observeEarnedPoints(childId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(costSnapshot), 0) FROM reward_redemptions WHERE childId = :childId")
    fun observeSpentPoints(childId: Long): Flow<Int>
}
