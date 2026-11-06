package com.primestrider.kotlinstarter.core.permission

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PermissionRequestTest {
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

    @Test
    fun a_permission_never_requested_is_askable() {
        assertEquals(PermissionStatus.ASKABLE, permissionStatusOf(app, permission))
        assertFalse(permissionRequested(app, permission))
    }

    @Test
    fun marking_a_request_makes_a_denial_without_rationale_blocked() {
        markPermissionRequested(app, permission)
        assertTrue(permissionRequested(app, permission))
        assertEquals(PermissionStatus.BLOCKED, permissionStatusOf(app, permission))
    }

    @Test
    fun a_granted_permission_is_granted_whatever_was_requested() {
        markPermissionRequested(app, permission)
        shadowOf(app).grantPermissions(permission)
        assertEquals(PermissionStatus.GRANTED, permissionStatusOf(app, permission))
    }

    @Test
    fun the_mark_is_shared_with_the_gate_store() {
        markPermissionRequested(app, permission)
        val stored = app.getSharedPreferences("permissions", Context.MODE_PRIVATE).getStringSet("requested", emptySet())
        assertEquals(setOf(permission), stored)
    }

    @Test
    fun launching_when_already_granted_reports_success_without_a_prompt() {
        shadowOf(app).grantPermissions(permission)
        val results = mutableListOf<Boolean>()
        lateinit var request: PermissionRequest
        rule.setContent { request = rememberPermissionRequest(permission) { results += it } }
        rule.runOnUiThread { request.launch() }
        rule.waitForIdle()
        assertEquals(PermissionStatus.GRANTED, request.status)
        assertEquals(listOf(true), results)
    }

    @Test
    fun the_status_of_a_request_reflects_a_blocked_permission() {
        markPermissionRequested(app, permission)
        lateinit var request: PermissionRequest
        rule.setContent { request = rememberPermissionRequest(permission) {} }
        rule.waitForIdle()
        assertEquals(PermissionStatus.BLOCKED, request.status)
    }
}
