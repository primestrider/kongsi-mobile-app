package com.primestrider.kotlinstarter.app

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.pip.PictureInPictureHost
import com.primestrider.kotlinstarter.feature.example.HOME_LIST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class PictureInPictureModeTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Inject
    lateinit var host: PictureInPictureHost

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        host.clear()
    }

    private fun pictureInPicture(on: Boolean) {
        composeRule.runOnUiThread {
            val activity = composeRule.activity
            activity.onPictureInPictureModeChanged(on, Configuration(activity.resources.configuration))
        }
        composeRule.waitForIdle()
    }

    @Test
    fun in_pip_only_the_host_content_shows_and_leaving_restores_the_back_stack() {
        // A page below Home, so the restore check covers the back stack and not just the start destination.
        composeRule.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("Colors"))
        composeRule.onNodeWithText("Colors").performClick()
        composeRule.onNodeWithText("Semantic colors").assertExists()
        composeRule.runOnUiThread { host.set { Text("PiP video") } }

        pictureInPicture(true)
        composeRule.onNodeWithText("PiP video").assertIsDisplayed()
        composeRule.onNodeWithText("Semantic colors").assertDoesNotExist()

        pictureInPicture(false)
        composeRule.onNodeWithText("PiP video").assertDoesNotExist()
        composeRule.onNodeWithText("Semantic colors").assertExists()
        composeRule.onNodeWithText("Colors").assertIsDisplayed()
    }

    @Test
    fun pip_without_host_content_keeps_the_shell() {
        pictureInPicture(true)
        composeRule.onNodeWithText("Kotlin Starter").assertIsDisplayed()
        pictureInPicture(false)
    }
}
