package com.baobei.growthplanet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baobei.growthplanet.settings.PlanetTheme
import com.baobei.growthplanet.ui.screens.*

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("今日成长", Icons.Default.AutoAwesome),
    REPORT("成长报告", Icons.Default.Insights),
    REWARD("奖励乐园", Icons.Default.CardGiftcard),
    PROFILE("我的", Icons.Default.Face),
}

@Composable
fun GrowthPlanetRoot(vm: AppViewModel, theme: PlanetTheme) {
    val children by vm.children.collectAsStateWithLifecycle()
    if (children.isEmpty()) {
        OnboardingScreen(onCreate = vm::createChild)
        return
    }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.TODAY -> TodayScreen(vm, theme, Modifier.padding(padding))
            Tab.REPORT -> ReportsScreen(vm, theme, Modifier.padding(padding))
            Tab.REWARD -> RewardsScreen(vm, Modifier.padding(padding))
            Tab.PROFILE -> ProfileScreen(vm, theme, Modifier.padding(padding))
        }
    }
}

