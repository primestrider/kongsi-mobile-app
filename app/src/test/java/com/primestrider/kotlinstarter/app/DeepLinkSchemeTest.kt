package com.primestrider.kotlinstarter.app

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.BuildConfig
import com.primestrider.kotlinstarter.core.common.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** dev installs next to prod, so it needs its own scheme or a deep link would open the other flavour (or a chooser). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DeepLinkSchemeTest {
    @Test
    fun dev_flavour_has_its_own_deep_link_scheme() {
        assertEquals("kotlinstarter-dev", BuildConfig.APP_SCHEME)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("${BuildConfig.APP_SCHEME}://example/sdk/clipboard"),
            ).setPackage(context.packageName)
        assertTrue(
            "the manifest must register ${BuildConfig.APP_SCHEME}",
            context.packageManager.queryIntentActivities(view, 0).isNotEmpty(),
        )
        // Modules that build links read the same value from the manifest.
        assertEquals(BuildConfig.APP_SCHEME, AppConfig.appScheme(context))
    }
}
