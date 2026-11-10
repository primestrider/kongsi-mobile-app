package com.primestrider.kongsi.feature.profile.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.primestrider.kongsi.core.permission.PermissionStatus
import com.primestrider.kongsi.core.permission.openAppSettings
import com.primestrider.kongsi.core.permission.rememberPermissionRequest
import com.primestrider.kongsi.feature.bill.PaymentAccountsRoute
import com.primestrider.kongsi.feature.profile.AboutRoute
import com.primestrider.kongsi.feature.profile.AppearanceRoute
import com.primestrider.kongsi.feature.profile.PermissionsRoute
import com.primestrider.kongsi.feature.profile.ProfileEditRoute

/** The Profile tab, rendered by the app shell; [onOpen] navigates to a route. */
@Composable
fun ProfileTab(onOpen: (Any) -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshLanguage()
        onPauseOrDispose {}
    }
    val context = LocalContext.current
    ProfileScreen(
        state = state,
        versionName = remember(context) { context.versionName() },
        actions =
            ProfileActions(
                onEditProfile = { onOpen(ProfileEditRoute) },
                onOpenPaymentAccounts = { onOpen(PaymentAccountsRoute) },
                onOpenPermissions = { onOpen(PermissionsRoute) },
                onOpenAppearance = { onOpen(AppearanceRoute) },
                onOpenAbout = { onOpen(AboutRoute) },
            ),
    )
}

/** Every page of the profile feature except the Profile tab itself. */
fun NavGraphBuilder.profileDestinations(navController: NavController) {
    composable<ProfileEditRoute> {
        val viewModel: ProfileEditViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(state.closed) {
            if (state.closed) navController.popBackStack()
        }
        ProfileEditScreen(
            state = state,
            onNameChange = viewModel::onNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onSave = viewModel::save,
            onBack = { navController.popBackStack() },
        )
    }
    composable<PermissionsRoute> {
        val context = LocalContext.current
        val requests = AppPermission.entries.associateWith { rememberPermissionRequest(it.permission) {} }
        PermissionsScreen(
            permissions =
                AppPermission.entries.map { permission -> PermissionRow(permission, requests.getValue(permission).status) },
            onAction = { permission, status ->
                // Android cannot revoke or re-ask from inside the app: granted and blocked go to the system settings.
                if (status == PermissionStatus.ASKABLE) {
                    requests.getValue(permission).launch()
                } else {
                    openAppSettings(context)
                }
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<AppearanceRoute> {
        val viewModel: ProfileViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.refreshLanguage()
            onPauseOrDispose {}
        }
        AppearanceScreen(
            themeMode = state.themeMode,
            language = state.language,
            onThemeModeChange = viewModel::setThemeMode,
            onLanguageChange = viewModel::setLanguage,
            onBack = { navController.popBackStack() },
        )
    }
    composable<AboutRoute> {
        val context = LocalContext.current
        AboutScreen(
            versionName = remember(context) { context.versionName() },
            onBack = { navController.popBackStack() },
        )
    }
}

private fun Context.versionName(): String =
    try {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
