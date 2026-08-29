package com.baobei.growthplanet.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class ChildProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String = "🚀",
    val theme: String = "SPACE",
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Entity(
    tableName = "behavior_categories",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("childId")],
)
data class BehaviorCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val emoji: String,
    val sortOrder: Int,
    val enabled: Boolean = true,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "behavior_items",
    foreignKeys = [
        ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(BehaviorCategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("childId"), Index("categoryId")],
)
data class BehaviorItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val categoryId: Long,
    val title: String,
    val sortOrder: Int,
    val maxScore: Int = 2,
    val enabled: Boolean = true,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "daily_records",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["childId", "date"], unique = true)],
)
data class DailyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val date: String,
    val confirmed: Boolean = false,
    val proudMoment: String = "",
    val encouragement: String = "",
    val confirmedAt: Long? = null,
)

@Entity(
    tableName = "behavior_scores",
    foreignKeys = [
        ForeignKey(DailyRecordEntity::class, ["id"], ["recordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(BehaviorItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [Index(value = ["recordId", "itemId"], unique = true), Index("itemId")],
)
data class BehaviorScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val itemId: Long,
    val selfScore: Int = -1,
    val parentScore: Int = -1,
    val itemTitleSnapshot: String,
    val categoryTitleSnapshot: String,
    val maxScoreSnapshot: Int,
)

@Entity(
    tableName = "deduction_types",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("childId")],
)
data class DeductionTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val points: Int,
    val enabled: Boolean = true,
    val custom: Boolean = false,
)

@Entity(
    tableName = "deduction_entries",
    foreignKeys = [
        ForeignKey(DailyRecordEntity::class, ["id"], ["recordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(DeductionTypeEntity::class, ["id"], ["typeId"], onDelete = ForeignKey.NO_ACTION),
    ],
    indices = [Index("recordId"), Index("typeId")],
)
data class DeductionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val typeId: Long,
    val titleSnapshot: String,
    val pointsSnapshot: Int,
    val count: Int = 1,
    val note: String = "",
)

@Entity(tableName = "weekly_goals", indices = [Index("childId")])
data class WeeklyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val weekStart: String,
    val title: String,
    val completed: Boolean = false,
)

@Entity(tableName = "rewards", indices = [Index("childId")])
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val emoji: String,
    val cost: Int,
    val sortOrder: Int,
    val enabled: Boolean = true,
)

@Entity(tableName = "reward_redemptions", indices = [Index("childId"), Index("rewardId")])
data class RewardRedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val rewardId: Long,
    val rewardTitleSnapshot: String,
    val costSnapshot: Int,
    val redeemedAt: Long = System.currentTimeMillis(),
)

data class CategoryWithItems(
    @androidx.room.Embedded val category: BehaviorCategoryEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "categoryId")
    val items: List<BehaviorItemEntity>,
)

data class RecordSummaryRow(
    val recordId: Long,
    val date: String,
    val positiveScore: Int,
    val maxScore: Int,
    val deduction: Int,
)

data class CategoryScoreRow(
    val category: String,
    val score: Int,
    val maxScore: Int,
)
