package com.baobei.growthplanet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.baobei.growthplanet.ui.screens.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createButtonRequiresNameAndReturnsSelectedAvatar() {
        var created = ""
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen { name, avatar -> created = "$name|$avatar" }
            }
        }

        composeRule.onNodeWithText("开启成长旅程").assertIsNotEnabled()
        composeRule.onNodeWithText("宝贝的小名").performTextInput("小星")
        composeRule.onNodeWithText("🦊").performClick()
        composeRule.onNodeWithText("开启成长旅程").assertIsEnabled().performClick()

        composeRule.runOnIdle { assertEquals("小星|🦊", created) }
    }
}
