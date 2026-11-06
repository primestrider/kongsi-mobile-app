package com.primestrider.kotlinstarter.feature.example

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.core.ui.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExamplePageScaffoldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun demo_section_title_is_a_heading() {
        rule.setContent { AppTheme(false) { DemoSection(R.string.crash_title) { Text("Body") } } }
        rule
            .onNodeWithText(
                ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.crash_title),
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Body").assertExists()
    }

    @Test
    fun demo_section_shows_usage_api_and_file() {
        rule.setContent {
            AppTheme(false) {
                DemoSection(R.string.crash_title, usage = R.string.crash_title, api = "anchoredDraggable", file = "DragScreen.kt") {
                    Text("Body")
                }
            }
        }
        rule.onNodeWithText("anchoredDraggable").assertExists()
        rule.onNodeWithText("File: DragScreen.kt").assertExists()
    }

    @Test
    fun demo_section_without_labels_shows_no_file() {
        rule.setContent { AppTheme(false) { DemoSection(R.string.crash_title) { Text("Body") } } }
        rule.onNodeWithText("File:", substring = true).assertDoesNotExist()
    }
}
