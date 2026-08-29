package com.baobei.growthplanet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChildProfileEntity::class, BehaviorCategoryEntity::class, BehaviorItemEntity::class,
        DailyRecordEntity::class, BehaviorScoreEntity::class, DeductionTypeEntity::class,
        DeductionEntryEntity::class, WeeklyGoalEntity::class, RewardEntity::class,
        RewardRedemptionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GrowthDatabase : RoomDatabase() {
    abstract fun dao(): GrowthDao

    companion object {
        @Volatile private var instance: GrowthDatabase? = null
        fun get(context: Context): GrowthDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                GrowthDatabase::class.java,
                "growth_planet.db",
            ).build().also { instance = it }
        }
    }
}
