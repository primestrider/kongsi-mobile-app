package com.primestrider.kotlinstarter.core.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStatusTest {
    @Test
    fun granted_wins_over_everything() {
        assertEquals(PermissionStatus.GRANTED, permissionStatus(granted = true, requestedBefore = false, shouldShowRationale = false))
        assertEquals(PermissionStatus.GRANTED, permissionStatus(granted = true, requestedBefore = true, shouldShowRationale = true))
    }

    @Test
    fun never_requested_is_askable() {
        assertEquals(PermissionStatus.ASKABLE, permissionStatus(granted = false, requestedBefore = false, shouldShowRationale = false))
    }

    @Test
    fun rationale_after_a_denial_is_still_askable() {
        assertEquals(PermissionStatus.ASKABLE, permissionStatus(granted = false, requestedBefore = true, shouldShowRationale = true))
    }

    @Test
    fun requested_before_without_rationale_is_blocked() {
        assertEquals(PermissionStatus.BLOCKED, permissionStatus(granted = false, requestedBefore = true, shouldShowRationale = false))
    }
}
