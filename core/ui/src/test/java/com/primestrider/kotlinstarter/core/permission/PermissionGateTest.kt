package com.primestrider.kotlinstarter.core.permission

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primestrider.kotlinstarter.core.designsystem.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PermissionGateTest {
    @get:Rule
    val rule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val permission = Manifest.permission.CAMERA

    @Before
    fun clearStore() {
        app
            .getSharedPreferences("permissions", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun show(owner: LifecycleOwner? = null) =
        rule.setContent {
            AppTheme(darkTheme = false) {
                val gate: @Composable () -> Unit = {
                    PermissionGate(permissions = listOf(permission), reason = "Camera is needed.") { Text("Inside") }
                }
                if (owner == null) gate() else CompositionLocalProvider(LocalLifecycleOwner provides owner) { gate() }
            }
        }

    @Test
    fun granted_shows_content() {
        shadowOf(app).grantPermissions(permission)
        show()
        rule.onNodeWithText("Inside").assertIsDisplayed()
    }

    @Test
    fun not_granted_shows_reason_and_grant_button() {
        shadowOf(app).denyPermissions(permission)
        show()
        rule.onNodeWithText("Camera is needed.").assertIsDisplayed()
        rule.onNodeWithText("Grant access").assertIsDisplayed()
        rule.onNodeWithText("Inside").assertDoesNotExist()
    }

    @Test
    fun requested_before_without_rationale_offers_settings() {
        shadowOf(app).denyPermissions(permission)
        app
            .getSharedPreferences("permissions", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("requested", setOf(permission))
            .commit()
        show()
        rule.onNodeWithText("Open Settings").assertIsDisplayed()
        rule.onNodeWithText("Camera is needed. Enable it in Settings to continue.").assertIsDisplayed()
        rule.onNodeWithText("Grant access").assertDoesNotExist()
    }

    private fun showAnyOf() =
        rule.setContent {
            AppTheme(darkTheme = false) {
                PermissionGate(
                    permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    reason = "Location is needed.",
                    requireAll = false,
                ) { Text("Inside") }
            }
        }

    @Test
    fun any_of_shows_content_with_only_coarse_granted() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        showAnyOf()
        rule.onNodeWithText("Inside").assertIsDisplayed()
    }

    @Test
    fun any_of_asks_when_none_is_granted() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        showAnyOf()
        rule.onNodeWithText("Grant access").assertIsDisplayed()
        rule.onNodeWithText("Inside").assertDoesNotExist()
    }

    @Test
    fun all_of_is_the_default_and_needs_every_permission() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        rule.setContent {
            AppTheme(darkTheme = false) {
                PermissionGate(
                    permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    reason = "Location is needed.",
                ) { Text("Inside") }
            }
        }
        rule.onNodeWithText("Inside").assertDoesNotExist()
    }

    @Test
    fun granting_in_settings_then_resuming_shows_content() {
        shadowOf(app).denyPermissions(permission)
        val owner =
            object : LifecycleOwner {
                val registry = LifecycleRegistry.createUnsafe(this)
                override val lifecycle: Lifecycle get() = registry
            }
        rule.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
        show(owner)
        rule.onNodeWithText("Grant access").assertIsDisplayed()

        shadowOf(app).grantPermissions(permission)
        rule.runOnUiThread {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Inside").assertIsDisplayed()
    }
}
