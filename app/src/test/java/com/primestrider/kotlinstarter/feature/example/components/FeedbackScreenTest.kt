package com.primestrider.kotlinstarter.feature.example.components

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.app.MainActivity
import com.primestrider.kotlinstarter.app.navigation.MainShell
import com.primestrider.kotlinstarter.core.designsystem.component.LocalMessenger
import com.primestrider.kotlinstarter.core.designsystem.component.Messenger
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import com.primestrider.kotlinstarter.feature.example.HOME_LIST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class FeedbackScreenTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject
    lateinit var messenger: Messenger

    @Test
    fun success_button_shows_a_message() {
        hiltRule.inject()
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalMessenger provides messenger) { AppTheme(darkTheme = false) { MainShell() } }
        }
        composeRule.onNode(hasText("Components") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Feedback"))
        composeRule.onNodeWithText("Feedback").performClick()
        composeRule.onNodeWithText("Success").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Saved. Your changes are live.").assertExists()
    }
}
