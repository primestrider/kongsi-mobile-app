package com.primestrider.kotlinstarter.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.BuildConfig
import com.primestrider.kotlinstarter.core.crash.CrashRecorder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class CrashDeepLinkTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Test
    fun a_deep_link_arriving_over_the_crash_fallback_does_not_crash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = CrashRecorder(context)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        try {
            recorder.install()
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), IllegalStateException("boom"))
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()

            val link =
                Intent(
                    Intent.ACTION_VIEW,
                    "${BuildConfig.APP_SCHEME}://example/sdk/payment-code?amount=50000".toUri(),
                    context,
                    MainActivity::class.java,
                )
            controller.newIntent(link)
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        } finally {
            controller.destroy()
        }
    }
}
