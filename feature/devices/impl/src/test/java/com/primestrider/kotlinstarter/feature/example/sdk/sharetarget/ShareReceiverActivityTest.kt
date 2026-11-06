package com.primestrider.kotlinstarter.feature.example.sdk.sharetarget

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ShareReceiverActivityTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private fun launch(intent: Intent) =
        ActivityScenario.launch<ShareReceiverActivity>(
            intent.setClass(ApplicationProvider.getApplicationContext(), ShareReceiverActivity::class.java),
        )

    @Test
    fun shared_text_is_shown() {
        launch(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Hello from another app")).use {
            rule.onNodeWithText("Hello from another app").assertExists()
        }
    }

    @Test
    fun nothing_usable_shows_the_empty_state() {
        launch(Intent(Intent.ACTION_SEND).setType("text/plain")).use {
            rule.onNodeWithText("Nothing to show").assertExists()
        }
    }
}
