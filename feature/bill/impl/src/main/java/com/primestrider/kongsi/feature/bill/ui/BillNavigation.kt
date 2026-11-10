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
import androidx.navigation.toRoute
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.NavBarAction
import com.primestrider.kongsi.core.designsystem.icon.AppIcons
import com.primestrider.kongsi.feature.bill.BillAssignRoute
import com.primestrider.kongsi.feature.bill.BillEditRoute
import com.primestrider.kongsi.feature.bill.BillPeopleRoute
import com.primestrider.kongsi.feature.bill.BillSummaryRoute
import com.primestrider.kongsi.feature.bill.PaymentAccountEditRoute
import com.primestrider.kongsi.feature.bill.PaymentAccountsRoute
import com.primestrider.kongsi.feature.bill.impl.R
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountEditActions
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountEditScreen
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountEditViewModel
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountsScreen
import com.primestrider.kongsi.feature.bill.ui.accounts.PaymentAccountsViewModel
import com.primestrider.kongsi.feature.bill.ui.assign.BillAssignActions
import com.primestrider.kongsi.feature.bill.ui.assign.BillAssignScreen
import com.primestrider.kongsi.feature.bill.ui.assign.BillAssignViewModel
import com.primestrider.kongsi.feature.bill.ui.flow.FlowStep
import com.primestrider.kongsi.feature.bill.ui.home.BillHomeScreen
import com.primestrider.kongsi.feature.bill.ui.home.BillHomeViewModel
import com.primestrider.kongsi.feature.bill.ui.people.BillPeopleActions
import com.primestrider.kongsi.feature.bill.ui.people.BillPeopleScreen
import com.primestrider.kongsi.feature.bill.ui.people.BillPeopleViewModel
import com.primestrider.kongsi.feature.bill.ui.review.BillReviewActions
import com.primestrider.kongsi.feature.bill.ui.review.BillReviewScreen
import com.primestrider.kongsi.feature.bill.ui.review.BillReviewViewModel
import com.primestrider.kongsi.feature.bill.ui.summary.BillSummaryActions
import com.primestrider.kongsi.feature.bill.ui.summary.BillSummaryScreen
import com.primestrider.kongsi.feature.bill.ui.summary.BillSummaryViewModel

/** The app's Home, rendered by `:app` on its `HomeRoute`; [onOpen] navigates to one of this feature's routes. */
@Composable
fun BillHome(onOpen: (Any) -> Unit) {
    val viewModel: BillHomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BillHomeScreen(
        state = state,
        onOpenPaymentAccounts = { onOpen(PaymentAccountsRoute) },
        onDeleteBill = viewModel::delete,
        onOpenBill = { id -> onOpen(BillEditRoute(id)) },
    )
}

/**
 * The app's main action, "New bill": `:app` shows it as the raised button in the middle of the navigation bar, so it
 * is one tap away from every tab. [onOpen] navigates to one of this feature's routes.
 */
@Composable
fun rememberNewBillAction(onOpen: (Any) -> Unit): NavBarAction {
    val label = stringResource(R.string.bill_home_new_bill)
    return remember(label, onOpen) { NavBarAction(label, AppIcons.ListAdd) { onOpen(BillEditRoute()) } }
}

/** Every page of the bill feature except Home. */
fun NavGraphBuilder.billDestinations(navController: NavController) {
    composable<BillEditRoute> {
        val viewModel: BillReviewViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val messenger = LocalMessenger.current
        BillReviewScreen(
            // The typed fields come straight from the ViewModel's Compose state (see BillReviewViewModel.title).
            state = state.copy(title = viewModel.title, charges = viewModel.charges),
            actions =
                BillReviewActions(
                    onBack = { navController.popBackStack() },
                    onTitleChange = viewModel::onTitleChange,
                    onChargeChange = viewModel::onChargeChange,
                    onRateChange = viewModel::onRateChange,
                    onAddItem = viewModel::startNewItem,
                    onEditItem = viewModel::editItem,
                    onEditorChange = viewModel::updateEditor,
                    onSaveItem = viewModel::saveItem,
                    onDeleteItem = viewModel::deleteItem,
                    onDismissEditor = viewModel::dismissEditor,
                    onWarningClick = viewModel::onWarningClick,
                    onFocusShown = viewModel::onFocusShown,
                    onNext = { navController.navigate(BillPeopleRoute(viewModel.currentBillId)) },
                ),
        )
    }
    composable<BillPeopleRoute> { entry ->
        val billId = entry.toRoute<BillPeopleRoute>().billId
        val viewModel: BillPeopleViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        BillPeopleScreen(
            state = state,
            query = viewModel.query,
            actions =
                BillPeopleActions(
                    onBack = { navController.popBackStack() },
                    onBackToItems = { navController.popBackStack() },
                    onQueryChange = viewModel::onQueryChange,
                    onContactsAllowed = viewModel::onContactsAllowed,
                    onAdd = viewModel::add,
                    onRemove = viewModel::remove,
                    onPickerResult = viewModel::onPickerResult,
                    onNext = { navController.navigate(BillAssignRoute(billId)) },
                ),
        )
    }
    composable<BillAssignRoute> { entry ->
        val billId = entry.toRoute<BillAssignRoute>().billId
        val viewModel: BillAssignViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        BillAssignScreen(
            state = state,
            actions =
                BillAssignActions(
                    onBack = { navController.popBackStack() },
                    onStepClick = { step ->
                        when (step) {
                            FlowStep.ITEMS -> navController.popBackStack<BillEditRoute>(inclusive = false)
                            FlowStep.PEOPLE -> navController.popBackStack()
                            else -> Unit
                        }
                    },
                    onModeChange = viewModel::onModeChange,
                    onSelectPerson = viewModel::onSelectPerson,
                    onToggle = viewModel::toggle,
                    onToggleEveryone = viewModel::toggleEveryone,
                    onSplitEqually = viewModel::splitEqually,
                    onUnassignedClick = viewModel::onUnassignedClick,
                    onFocusShown = viewModel::onFocusShown,
                    onNext = { navController.navigate(BillSummaryRoute(billId)) },
                ),
        )
    }
    composable<BillSummaryRoute> {
        val viewModel: BillSummaryViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        BillSummaryScreen(
            state = state,
            actions =
                BillSummaryActions(
                    onBack = { navController.popBackStack() },
                    onStepClick = { step ->
                        when (step) {
                            FlowStep.ITEMS -> navController.popBackStack<BillEditRoute>(inclusive = false)
                            FlowStep.PEOPLE -> navController.popBackStack<BillPeopleRoute>(inclusive = false)
                            FlowStep.ASSIGN -> navController.popBackStack()
                            FlowStep.SUMMARY -> Unit
                        }
                    },
                    onOpenBreakdown = viewModel::onOpenBreakdown,
                    onCloseBreakdown = viewModel::onCloseBreakdown,
                    onDone = {
                        viewModel.finish()
                        // Back to Home: the whole flow of this bill leaves the back stack.
                        navController.popBackStack<BillEditRoute>(inclusive = true)
                    },
                ),
        )
    }
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
