package com.baobei.growthplanet

import android.app.Application
import com.baobei.growthplanet.data.GrowthDatabase
import com.baobei.growthplanet.data.GrowthRepository
import com.baobei.growthplanet.reminder.ReminderScheduler
import com.baobei.growthplanet.settings.AppSettings

class GrowthPlanetApp : Application() {
    lateinit var repository: GrowthRepository
        private set
    lateinit var settings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GrowthRepository(GrowthDatabase.get(this))
        settings = AppSettings(this)
        ReminderScheduler.createChannel(this)
    }
}
