package com.primestrider.kotlinstarter.feature.example.auth

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.primestrider.kotlinstarter.feature.example.AccountRoute
import com.primestrider.kotlinstarter.feature.example.ProfileRoute
import com.primestrider.kotlinstarter.feature.example.SettingsRoute
import com.primestrider.kotlinstarter.feature.example.SignInRoute

/**
 * The Profile hub, sign-in and the protected Account page.
 * Account guard: once the stored session has been read, a missing session while Account is showing (expiry, or none
 * stored when Account is restored) replaces Account with sign-in, so back from sign-in never returns to Account.
 * A user sign-out returns to the Profile hub instead.
 */
fun NavGraphBuilder.accountDestinations(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<ProfileRoute> {
        // The account state holder doubles as the session source for the hub.
        val viewModel: AccountViewModel = hiltViewModel()
        val session by viewModel.session.collectAsStateWithLifecycle()
        ProfileScreen(
            session = session,
            onSignIn = { navController.navigate(SignInRoute) },
            onOpenAccount = { navController.navigate(AccountRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
        )
    }
    composable<SignInRoute> {
        val viewModel: SignInViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) {
            viewModel.signedIn.collect {
                navController.navigate(AccountRoute) { popUpTo<SignInRoute> { inclusive = true } }
            }
        }
        SignInScreen(
            state = state,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onShortLivedChange = viewModel::onShortLivedChange,
            onFillDemo = viewModel::fillDemo,
            onSubmit = viewModel::submit,
            onBack = back,
        )
    }
    composable<AccountRoute> {
        val viewModel: AccountViewModel = hiltViewModel()
        val session by viewModel.session.collectAsStateWithLifecycle()
        val profile by viewModel.profile.collectAsStateWithLifecycle()
        val signingOut by viewModel.signingOut.collectAsStateWithLifecycle()
        val loaded by viewModel.loaded.collectAsStateWithLifecycle()
        val current = session
        LaunchedEffect(viewModel) {
            viewModel.signedOut.collect { navController.popBackStack(ProfileRoute, inclusive = false) }
        }
        // A session lost without the user asking (expiry) is replaced by sign-in; a deliberate sign-out returns to Profile.
        // Before the stored session is read (Account restored after process death) there is nothing to judge yet.
        LaunchedEffect(loaded, current == null, signingOut) {
            if (loaded && current == null && !signingOut) {
                navController.navigate(SignInRoute) { popUpTo<AccountRoute> { inclusive = true } }
            }
        }
        if (current != null) {
            AccountScreen(
                session = current,
                profile = profile,
                onLoadProfile = viewModel::loadProfile,
                onSignOut = viewModel::signOut,
                onBack = back,
            )
        }
    }
}
