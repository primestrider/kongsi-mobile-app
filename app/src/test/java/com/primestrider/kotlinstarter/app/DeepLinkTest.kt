package com.primestrider.kotlinstarter.app

// remove-examples: exercises the example features, so scripts/remove-examples.sh deletes it with them.

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.BuildConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36], qualifiers = "in")
class DeepLinkTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    private fun launch(uri: String): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri(), context, MainActivity::class.java)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun payment_code_link_opens_the_screen_with_the_amount() {
        launch("${BuildConfig.APP_SCHEME}://example/sdk/payment-code?amount=50000").use {
            composeRule.onNodeWithText("Tunjukkan kode ini ke kasir").assertIsDisplayed()
            composeRule.onNodeWithText("Jumlah Rp50.000").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun a_link_without_amount_opens_the_screen_without_the_amount_row() {
        launch("${BuildConfig.APP_SCHEME}://example/sdk/payment-code").use {
            composeRule.onNodeWithText("Tunjukkan kode ini ke kasir").assertIsDisplayed()
            composeRule.onAllNodes(hasText("Jumlah", substring = true)).assertCountEquals(0)
        }
    }

    @Test
    fun a_negative_amount_in_the_link_is_ignored() {
        launch("${BuildConfig.APP_SCHEME}://example/sdk/payment-code?amount=-50000").use {
            composeRule.onNodeWithText("Tunjukkan kode ini ke kasir").assertIsDisplayed()
            composeRule.onAllNodes(hasText("Jumlah", substring = true)).assertCountEquals(0)
        }
    }

    @Test
    fun todos_link_from_the_home_screen_widget_opens_todos() {
        launch("${BuildConfig.APP_SCHEME}://example/todos").use {
            // The input hint is only on the Todos page; the page reads Room first, so wait for it.
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodes(hasText("Apa yang perlu dikerjakan?")).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
