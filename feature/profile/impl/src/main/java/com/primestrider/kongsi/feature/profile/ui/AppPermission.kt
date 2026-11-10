package com.primestrider.kongsi.feature.profile.ui

import android.Manifest
import androidx.annotation.StringRes
import com.primestrider.kongsi.core.permission.PermissionStatus
import com.primestrider.kongsi.feature.profile.impl.R

/** What the app may ask access to, as listed under "Permissions" in Profile. */
enum class AppPermission(
    val permission: String,
    @StringRes val title: Int,
    @StringRes val purpose: Int,
) {
    CONTACTS(Manifest.permission.READ_CONTACTS, R.string.profile_permission_contacts, R.string.profile_permission_contacts_purpose),

    /** Receipts are photographed with the camera inside the app (the bill feature's `ReceiptCamera`). */
    CAMERA(Manifest.permission.CAMERA, R.string.profile_permission_camera, R.string.profile_permission_camera_purpose),
}

/** One row of the permissions section. */
data class PermissionRow(
    val permission: AppPermission,
    val status: PermissionStatus,
)
