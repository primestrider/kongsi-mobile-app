package com.primestrider.kotlinstarter.core.permission

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Where [permission] stands for [context]: granted, askable, or blocked (asked before and the system will not prompt again). */
fun permissionStatusOf(
    context: Context,
    permission: String,
): PermissionStatus {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return evaluate(context, prefs, listOf(permission), requireAll = true)
}

/** Records that [permission] is about to be requested, so a later denial without a rationale reads as [PermissionStatus.BLOCKED]. */
fun markPermissionRequested(
    context: Context,
    permission: String,
) {
    markRequested(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE), listOf(permission))
}

/** A permission requested when the user presses something (no gate card): [launch] asks, [status] follows the result and `ON_RESUME`. */
class PermissionRequest internal constructor(
    val status: PermissionStatus,
    private val request: () -> Unit,
) {
    /**
     * Launches the system prompt; when the permission is already granted, calls the `onResult` given to
     * [rememberPermissionRequest] with true right away instead.
     */
    fun launch() = request()
}

/**
 * Press-time counterpart of [PermissionGate], sharing its bookkeeping: [PermissionRequest.launch] records the request in
 * `SharedPreferences("permissions")` before prompting, so [PermissionRequest.status] becomes BLOCKED after a denial the
 * system will not repeat, and a [PermissionGate] for the same permission shows "Open Settings" instead of a dead button.
 * [onResult] gets the outcome of each prompt (and true, without a prompt, when already granted).
 */
@Composable
fun rememberPermissionRequest(
    permission: String,
    onResult: (granted: Boolean) -> Unit,
): PermissionRequest {
    val context = LocalContext.current
    var status by remember(permission) { mutableStateOf(permissionStatusOf(context, permission)) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            status = permissionStatusOf(context, permission)
            onResult(granted)
        }
    LifecycleResumeEffect(permission) {
        status = permissionStatusOf(context, permission)
        onPauseOrDispose {}
    }
    return PermissionRequest(status) {
        if (status == PermissionStatus.GRANTED) {
            onResult(true)
        } else {
            markPermissionRequested(context, permission)
            launcher.launch(permission)
        }
    }
}
