package com.primestrider.kongsi.feature.bill.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.NavBarAction
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.core.designsystem.icon.AppIcons
import com.primestrider.kongsi.feature.bill.PaymentAccountEditRoute
import com.primestrider.kongsi.feature.bill.PaymentAccountsRoute
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountEditActions
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountEditScreen
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountEditViewModel
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountsScreen
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountsViewModel
import com.primestrider.kongsi.feature.bill.ui.home.BillHomeScreen
import com.primestrider.kongsi.feature.bill.ui.home.BillHomeViewModel

/** The app's Home, rendered by `:app` on its `HomeRoute`; [onOpen] navigates to one of this feature's routes. */
@Composable
fun BillHome(onOpen: (Any) -> Unit) {
    val viewModel: BillHomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BillHomeScreen(
        state = state,
        onOpenPaymentAccounts = { onOpen(PaymentAccountsRoute) },
        onDeleteBill = viewModel::delete,
    )
}

/**
 * The app's main action, "New bill": `:app` shows it as the raised button in the middle of the navigation bar, so it
 * is one tap away from every tab. [onOpen] navigates to one of this feature's routes.
 */
@Composable
fun rememberNewBillAction(onOpen: (Any) -> Unit): NavBarAction {
    val messenger = LocalMessenger.current
    val label = stringResource(R.string.bill_home_new_bill)
    return remember(label, messenger, onOpen) {
        // Bill creation arrives with the next tasks of the plan (docs/superpowers/plans/2026-11-06-kongsi-split-bill.md).
        NavBarAction(label, AppIcons.ListAdd) { messenger.info(UiText.Resource(R.string.bill_home_new_bill_soon)) }
    }
}

/** Every page of the bill feature except Home. */
fun NavGraphBuilder.billDestinations(navController: NavController) {
    composable<PaymentAccountsRoute> {
        val viewModel: PaymentAccountsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        PaymentAccountsScreen(
            state = state,
            onAdd = { navController.navigate(PaymentAccountEditRoute()) },
            onEdit = { id -> navController.navigate(PaymentAccountEditRoute(id)) },
            onDelete = viewModel::delete,
            onBack = { navController.popBackStack() },
        )
    }
    composable<PaymentAccountEditRoute> {
        val viewModel: PaymentAccountEditViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(state.closed) {
            if (state.closed) navController.popBackStack()
        }
        PaymentAccountEditScreen(
            state = state,
            actions =
                PaymentAccountEditActions(
                    onTypeChange = viewModel::onTypeChange,
                    onProviderChange = viewModel::onProviderChange,
                    onOtherProviderChange = viewModel::onOtherProviderChange,
                    onNumberChange = viewModel::onNumberChange,
                    onUseMyNumber = viewModel::onUseMyNumber,
                    onHolderNameChange = viewModel::onHolderNameChange,
                    onDefaultChange = viewModel::onDefaultChange,
                    onSave = viewModel::save,
                    onBack = { navController.popBackStack() },
                ),
        )
    }
}
