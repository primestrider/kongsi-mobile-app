package com.primestrider.kotlinstarter.architecture

import com.primestrider.kotlinstarter.core.auth.AuthAdapter
import com.primestrider.kotlinstarter.feature.example.auth.DummyJsonAuthAdapter
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/** With the account feature wired, its DummyJSON adapter replaces core:auth's "not configured" fallback. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class AuthAdapterBindingTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var adapter: AuthAdapter

    @Before
    fun inject() = hilt.inject()

    @Test
    fun account_feature_replaces_not_configured_adapter() {
        assertTrue("got ${adapter::class}", adapter is DummyJsonAuthAdapter)
    }
}
