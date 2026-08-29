package com.baobei.growthplanet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.baobei.growthplanet.reminder.ReminderScheduler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baobei.growthplanet.audio.MusicController
import com.baobei.growthplanet.settings.PlanetTheme
import com.baobei.growthplanet.ui.AppViewModel
import com.baobei.growthplanet.ui.GrowthPlanetRoot
import com.baobei.growthplanet.ui.theme.GrowthPlanetTheme

class MainActivity : AppCompatActivity() {
    private lateinit var music: MusicController
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        finishNotificationPermission(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        music = MusicController(this)
        val app = application as GrowthPlanetApp
        configureNotifications(app)
        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.Factory(app.repository, app.settings))
            val child by vm.selectedChild.collectAsStateWithLifecycle()
            val enabled by vm.musicEnabled.collectAsStateWithLifecycle()
            val theme = runCatching { PlanetTheme.valueOf(child?.theme ?: "SPACE") }.getOrDefault(PlanetTheme.SPACE)
            LaunchedEffect(enabled, theme) { music.setEnabled(enabled); music.play(theme) }
            GrowthPlanetTheme(theme) { GrowthPlanetRoot(vm, theme) }
        }
    }

    private fun configureNotifications(app: GrowthPlanetApp) {
        ReminderScheduler.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            when {
                granted -> {
                    if (!app.settings.notificationPermissionInitialized) app.settings.setReminder(true)
                    app.settings.markNotificationPermissionInitialized()
                    if (app.settings.reminderEnabled.value) {
                        ReminderScheduler.schedule(this, app.settings.reminderHour.value, app.settings.reminderMinute.value)
                    }
                }
                !app.settings.notificationPermissionInitialized -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> {
                    app.settings.setReminder(false)
                    ReminderScheduler.cancel(this)
                }
            }
        } else {
            if (!app.settings.notificationPermissionInitialized) app.settings.setReminder(true)
            app.settings.markNotificationPermissionInitialized()
            if (app.settings.reminderEnabled.value) {
                ReminderScheduler.schedule(this, app.settings.reminderHour.value, app.settings.reminderMinute.value)
            }
        }
    }

    private fun finishNotificationPermission(granted: Boolean) {
        val app = application as GrowthPlanetApp
        app.settings.markNotificationPermissionInitialized()
        app.settings.setReminder(granted)
        if (granted) {
            ReminderScheduler.schedule(this, app.settings.reminderHour.value, app.settings.reminderMinute.value)
        } else {
            ReminderScheduler.cancel(this)
        }
    }

    override fun onStart() { super.onStart(); if (::music.isInitialized) music.resume() }
    override fun onStop() { if (::music.isInitialized) music.onStop(); super.onStop() }
    override fun onDestroy() { if (::music.isInitialized) music.release(); super.onDestroy() }
}
