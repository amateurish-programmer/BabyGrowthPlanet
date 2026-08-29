package com.baobei.growthplanet.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.baobei.growthplanet.data.DeductionTypeEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DeductionPickerDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deductionItemsRenderAndCanBeSelected() {
        var selectedId = -1L
        val types = (1L..12L).map { id ->
            DeductionTypeEntity(id = id, childId = 1, title = "扣分规则$id", points = 2)
        }

        composeRule.setContent {
            MaterialTheme {
                DeductionPickerDialog(
                    types = types,
                    onDismiss = {},
                    onSelect = { selectedId = it.id },
                )
            }
        }

        composeRule.onNodeWithText("选择扣分项").assertIsDisplayed()
        composeRule.onNodeWithText("扣分规则1").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1L, selectedId) }
    }
}