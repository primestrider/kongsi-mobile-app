package com.primestrider.kongsi.feature.profile.ui

import android.Manifest
import androidx.annotation.StringRes
import com.primestrider.kongsi.core.permission.PermissionStatus
import com.primestrider.kongsi.feature.profile.impl.R

/**
 * What the app may ask access to, as listed under "Permissions" in Profile. [permission] is null for access the app
 * gets without a runtime permission (shown so the user knows it is not needed).
 */
enum class AppPermission(
    val permission: String?,
    @StringRes val title: Int,
    @StringRes val purpose: Int,
) {
    CONTACTS(Manifest.permission.READ_CONTACTS, R.string.profile_permission_contacts, R.string.profile_permission_contacts_purpose),

    /** Receipts are photographed by the phone's own camera app (`TakePicture`), which needs no permission. */
    CAMERA(null, R.string.profile_permission_camera, R.string.profile_permission_camera_purpose),
}

/** One row of the permissions section; [status] is null when no permission is needed. */
data class PermissionRow(
    val permission: AppPermission,
    val status: PermissionStatus?,
)
