package com.primestrider.kongsi.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kongsi.app.navigation.MainShell
import com.primestrider.kongsi.app.navigation.homeTab
import com.primestrider.kongsi.core.designsystem.component.APP_NAVIGATION_BAR_TAG
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.Messenger
import com.primestrider.kongsi.core.designsystem.theme.AppTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

/** What is left without any feature: Home is the placeholder and, with a single tab, the bar hides. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class AppTabsTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject
    lateinit var messenger: Messenger

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalMessenger provides messenger) {
                AppTheme(darkTheme = false) { MainShell(tabs = listOf(homeTab(screens = emptyList()))) }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun empty_feature_tabs_fall_back_to_placeholder_home() {
        composeRule.onNodeWithText("Start building here").assertExists()
    }

    @Test
    fun single_tab_hides_bottom_bar() {
        composeRule.onNodeWithTag(APP_NAVIGATION_BAR_TAG).assertDoesNotExist()
    }
}
