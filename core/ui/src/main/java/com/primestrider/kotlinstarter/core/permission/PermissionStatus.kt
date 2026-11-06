package com.primestrider.kotlinstarter.core.permission

/** Where a runtime permission stands: usable, can still be asked for, or only the system settings can grant it. */
enum class PermissionStatus { GRANTED, ASKABLE, BLOCKED }

/**
 * Android never says "permanently denied" directly. A permission that was requested before and for which
 * the system no longer wants a rationale will not show the prompt again, so it is [PermissionStatus.BLOCKED].
 */
fun permissionStatus(
    granted: Boolean,
    requestedBefore: Boolean,
    shouldShowRationale: Boolean,
): PermissionStatus =
    when {
        granted -> PermissionStatus.GRANTED
        !requestedBefore || shouldShowRationale -> PermissionStatus.ASKABLE
        else -> PermissionStatus.BLOCKED
    }
