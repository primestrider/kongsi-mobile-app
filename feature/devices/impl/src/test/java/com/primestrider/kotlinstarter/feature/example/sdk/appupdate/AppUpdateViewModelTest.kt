package com.primestrider.kotlinstarter.feature.example.sdk.appupdate

import android.content.Context
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppUpdateViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fake = FakeAppUpdateManager(context)

    /** The flow is started through a launcher; the fake does not launch anything. */
    private val launcher =
        object : ActivityResultLauncher<IntentSenderRequest>() {
            override val contract: ActivityResultContract<IntentSenderRequest, *> = ActivityResultContracts.StartIntentSenderForResult()

            override fun launch(
                input: IntentSenderRequest,
                options: ActivityOptionsCompat?,
            ) = Unit

            override fun unregister() = Unit
        }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AppUpdateViewModel(fake).also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun no_update_is_up_to_date() {
        assertEquals(AppUpdateState.UpToDate, viewModel().state.value)
    }

    @Test
    fun available_update_is_offered() {
        fake.setUpdateAvailable(42, AppUpdateType.FLEXIBLE)
        assertEquals(AppUpdateState.Available(versionCode = 42), viewModel().state.value)
    }

    @Test
    fun flexible_flow_reports_progress_then_completes() {
        fake.setUpdateAvailable(42, AppUpdateType.FLEXIBLE)
        val viewModel = viewModel()
        viewModel.startUpdate(launcher)
        idle()
        assertTrue(fake.isConfirmationDialogVisible)
        fake.userAcceptsUpdate()
        fake.downloadStarts()
        fake.setTotalBytesToDownload(1_000)
        fake.setBytesDownloaded(250)
        idle()
        assertEquals(AppUpdateState.Downloading(bytes = 250, total = 1_000), viewModel.state.value)
        fake.downloadCompletes()
        idle()
        assertEquals(AppUpdateState.Downloaded, viewModel.state.value)
        viewModel.completeUpdate()
        idle()
        assertTrue(fake.isInstallSplashScreenVisible)
    }

    @Test
    fun declined_update_is_offered_again() {
        fake.setUpdateAvailable(42, AppUpdateType.FLEXIBLE)
        val viewModel = viewModel()
        viewModel.startUpdate(launcher)
        idle()
        fake.userRejectsUpdate()
        idle()
        assertEquals(AppUpdateState.Available(versionCode = 42), viewModel.state.value)
    }

    @Test
    fun not_installed_from_play_is_unavailable() {
        // What a debug build or a sideloaded APK gets from Play.
        fake.setInstallErrorCode(InstallErrorCode.ERROR_APP_NOT_OWNED)
        val state = viewModel().state.value
        assertTrue("$state", state is AppUpdateState.Unavailable && state.message.contains("not owned"))
    }

    @Test
    fun failed_download_is_reported() {
        fake.setUpdateAvailable(42, AppUpdateType.FLEXIBLE)
        val viewModel = viewModel()
        viewModel.startUpdate(launcher)
        idle()
        fake.userAcceptsUpdate()
        fake.downloadStarts()
        fake.downloadFails()
        idle()
        assertTrue("${viewModel.state.value}", viewModel.state.value is AppUpdateState.Failed)
    }

    @Test
    fun cancelled_dialog_checks_again_so_update_can_restart() {
        fake.setUpdateAvailable(42, AppUpdateType.FLEXIBLE)
        val viewModel = viewModel()
        viewModel.startUpdate(launcher)
        idle()
        fake.userRejectsUpdate()
        viewModel.onUpdateFlowResult(android.app.Activity.RESULT_CANCELED)
        idle()
        assertEquals(AppUpdateState.Available(versionCode = 42), viewModel.state.value)
        viewModel.startUpdate(launcher)
        idle()
        assertTrue(fake.isConfirmationDialogVisible)
    }
}
