package com.choice.app.ui.templates

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class TemplatePickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun templatePicker_showsFourTemplates() {
        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = { _, _ -> },
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Workout").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movie").assertIsDisplayed()
    }

    @Test
    fun templatePicker_showsStartBlankOption() {
        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = { _, _ -> },
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Start Blank").assertIsDisplayed()
    }

    @Test
    fun templatePicker_clickingTemplate_callsOnTemplateSelected() {
        var selectedName = ""
        var selectedChoices = emptyList<String>()

        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = { name, choices ->
                    selectedName = name
                    selectedChoices = choices
                },
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Breakfast").performClick()

        assert(selectedName == "Breakfast") { "Expected Breakfast, got $selectedName" }
        assert(selectedChoices == listOf("Ful", "Eggs", "Falafel", "Cheese")) {
            "Expected correct choices, got $selectedChoices"
        }
    }

    @Test
    fun templatePicker_clickingStartBlank_callsOnStartBlank() {
        var blankCalled = false

        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = { _, _ -> },
                onStartBlank = { blankCalled = true },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Start Blank").performClick()

        assert(blankCalled) { "Expected onStartBlank to be called" }
    }

    @Test
    fun templatePicker_showsCreateCoinTitle() {
        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = { _, _ -> },
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Create Coin").assertIsDisplayed()
    }
}
