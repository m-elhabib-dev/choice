package com.choice.app.ui.templates

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.choice.app.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TemplatePickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun templatePicker_showsFourTemplates() {
        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = {},
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.template_breakfast_name)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.template_lunch_name)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.template_workout_name)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.template_movie_name)).assertIsDisplayed()
    }

    @Test
    fun templatePicker_showsStartBlankOption() {
        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = {},
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.templates_start_blank_title)).assertIsDisplayed()
    }

    @Test
    fun templatePicker_clickingTemplate_callsOnTemplateSelectedWithStableId() {
        var selectedTemplateId = ""

        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = { templateId -> selectedTemplateId = templateId },
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.template_breakfast_name)).performClick()

        assert(selectedTemplateId == "breakfast") {
            "Expected the stable id \"breakfast\", got \"$selectedTemplateId\""
        }
    }

    @Test
    fun templatePicker_clickingStartBlank_callsOnStartBlank() {
        var blankCalled = false

        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = {},
                onStartBlank = { blankCalled = true },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.templates_start_blank_title)).performClick()

        assert(blankCalled) { "Expected onStartBlank to be called" }
    }

    @Test
    fun templatePicker_showsCreateCoinTitle() {
        composeTestRule.setContent {
            TemplatePickerScreen(
                onTemplateSelected = {},
                onStartBlank = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.templates_title)).assertIsDisplayed()
    }
}
