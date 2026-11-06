package com.primestrider.kotlinstarter.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The app id of the variant under test; the baseline profile plugin passes it (the dev flavour adds `.dev`). */
internal val targetAppId: String
    get() = InstrumentationRegistry.getArguments().getString("targetAppId") ?: error("targetAppId argument missing")

/** Startup journey: cold start to Home, then open the first example list if the catalog is wired. */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = targetAppId, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.findObject(By.text("Products")), 5_000)?.click()
            device.waitForIdle()
        }
}
