package com.baobei.growthplanet.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlanetTheme { SPACE, FOREST, RAINBOW }

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("growth_planet_settings", Context.MODE_PRIVATE)
    private val _musicEnabled = MutableStateFlow(prefs.getBoolean("music_enabled", true))
    private val _selectedChild = MutableStateFlow(prefs.getLong("selected_child", 0L))
    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean("reminder_enabled", false))
    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 20))
    private val _reminderMinute = MutableStateFlow(prefs.getInt("reminder_minute", 0))

    val musicEnabled = _musicEnabled.asStateFlow()
    val selectedChild = _selectedChild.asStateFlow()
    val reminderEnabled = _reminderEnabled.asStateFlow()
    val reminderHour = _reminderHour.asStateFlow()
    val reminderMinute = _reminderMinute.asStateFlow()
    val notificationPermissionInitialized: Boolean
        get() = prefs.getBoolean("notification_permission_initialized", false)

    fun setMusicEnabled(value: Boolean) {
        _musicEnabled.value = value
        prefs.edit { putBoolean("music_enabled", value) }
    }

    fun setSelectedChild(id: Long) {
        _selectedChild.value = id
        prefs.edit { putLong("selected_child", id) }
    }

    fun markNotificationPermissionInitialized() {
        prefs.edit { putBoolean("notification_permission_initialized", true) }
    }

    fun setReminder(enabled: Boolean, hour: Int = _reminderHour.value, minute: Int = _reminderMinute.value) {
        _reminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit {
            putBoolean("reminder_enabled", enabled)
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
        }
    }
}
