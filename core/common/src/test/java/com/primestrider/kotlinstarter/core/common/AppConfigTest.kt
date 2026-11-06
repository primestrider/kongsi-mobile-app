package com.primestrider.kotlinstarter.core.common

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppConfigTest {
    @Test
    fun app_scheme_falls_back_without_the_app_meta_data() {
        // This module's test manifest has no <meta-data>; :app's DeepLinkSchemeTest checks the published value.
        assertEquals("kotlinstarter", AppConfig.appScheme(ApplicationProvider.getApplicationContext()))
    }
}
