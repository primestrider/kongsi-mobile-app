package com.primestrider.kotlinstarter.core.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.ui.R
import com.primestrider.kotlinstarter.core.util.findActivity

internal const val PREFS_NAME = "permissions"
private const val REQUESTED_KEY = "requested"

/**
 * Shows [content] once every permission in [permissions] is granted (any one of them when [requireAll] is false, e.g. fine or
 * coarse location; the system prompt still asks for all of them); until then an outlined card explains
 * [reason] and offers "Grant access", or "Open Settings" when the system will no longer show the prompt.
 *
 * Which permissions were requested is kept in `SharedPreferences("permissions")`, so a blocked permission is
 * still recognised after a restart. The state is re-read on every `ON_RESUME`, so granting the permission in
 * the system settings and coming back shows the content.
 */
@Composable
fun PermissionGate(
    permissions: List<String>,
    reason: String,
    modifier: Modifier = Modifier,
    requireAll: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var status by remember { mutableStateOf(evaluate(context, prefs, permissions, requireAll)) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            status = evaluate(context, prefs, permissions, requireAll)
        }
    LifecycleResumeEffect(permissions) {
        status = evaluate(context, prefs, permissions, requireAll)
        onPauseOrDispose {}
    }

    if (status == PermissionStatus.GRANTED) {
        content()
        return
    }
    val blocked = status == PermissionStatus.BLOCKED
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            val text = if (blocked) "$reason ${stringResource(R.string.permission_blocked_suffix)}" else reason
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (blocked) {
                AppButton(stringResource(R.string.permission_open_settings), onClick = { openAppSettings(context) })
            } else {
                AppButton(
                    stringResource(R.string.permission_grant),
                    onClick = {
                        markRequested(prefs, permissions)
                        launcher.launch(permissions.toTypedArray())
                    },
                )
            }
        }
    }
}

internal fun evaluate(
    context: Context,
    prefs: SharedPreferences,
    permissions: List<String>,
    requireAll: Boolean,
): PermissionStatus {
    val notGranted = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    // Any-of: one granted permission is enough, so nothing is missing; the request still asks for all of them.
    val missing = if (!requireAll && notGranted.size < permissions.size) emptyList() else notGranted
    val requested = prefs.getStringSet(REQUESTED_KEY, emptySet()).orEmpty()
    val activity = context.findActivity()
    return permissionStatus(
        granted = missing.isEmpty(),
        requestedBefore = missing.all { it in requested },
        shouldShowRationale = activity != null && missing.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) },
    )
}

internal fun markRequested(
    prefs: SharedPreferences,
    permissions: List<String>,
) {
    val requested = prefs.getStringSet(REQUESTED_KEY, emptySet()).orEmpty()
    // A new set: the one SharedPreferences hands out must not be modified in place.
    prefs.edit().putStringSet(REQUESTED_KEY, requested + permissions).apply()
}

/** Opens this app's page in the system settings; does nothing when no settings app can handle it. */
fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No settings app on this device; nothing else to offer.
    }
}

/** Whether [PermissionGate] has asked for [permission] before: tells "denied" from "not requested" for a permission that is not granted. */
fun permissionRequested(
    context: Context,
    permission: String,
): Boolean =
    permission in
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(REQUESTED_KEY, emptySet())
            .orEmpty()
