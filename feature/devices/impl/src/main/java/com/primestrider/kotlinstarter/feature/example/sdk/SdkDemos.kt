package com.primestrider.kotlinstarter.feature.example.sdk

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogEntry
import com.primestrider.kotlinstarter.feature.catalog.api.CatalogSections
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.SdkDemoRoute
import com.primestrider.kotlinstarter.feature.example.sdk.appinfo.AppInfoScreen
import com.primestrider.kotlinstarter.feature.example.sdk.appupdate.AppUpdateScreen
import com.primestrider.kotlinstarter.feature.example.sdk.background.BackgroundSyncScreen
import com.primestrider.kotlinstarter.feature.example.sdk.biometric.BiometricScreen
import com.primestrider.kotlinstarter.feature.example.sdk.blob.BlobScreen
import com.primestrider.kotlinstarter.feature.example.sdk.browser.WebBrowserScreen
import com.primestrider.kotlinstarter.feature.example.sdk.calendar.CalendarScreen
import com.primestrider.kotlinstarter.feature.example.sdk.camera.CameraScreen
import com.primestrider.kotlinstarter.feature.example.sdk.clipboard.ClipboardScreen
import com.primestrider.kotlinstarter.feature.example.sdk.contacts.ContactsScreen
import com.primestrider.kotlinstarter.feature.example.sdk.device.DeviceScreen
import com.primestrider.kotlinstarter.feature.example.sdk.effects.EffectsScreen
import com.primestrider.kotlinstarter.feature.example.sdk.files.FilesScreen
import com.primestrider.kotlinstarter.feature.example.sdk.integrity.IntegrityScreen
import com.primestrider.kotlinstarter.feature.example.sdk.livephoto.LivePhotoScreen
import com.primestrider.kotlinstarter.feature.example.sdk.location.LocationScreen
import com.primestrider.kotlinstarter.feature.example.sdk.media.MediaScreen
import com.primestrider.kotlinstarter.feature.example.sdk.music.MusicScreen
import com.primestrider.kotlinstarter.feature.example.sdk.notifications.NotificationsScreen
import com.primestrider.kotlinstarter.feature.example.sdk.payment.PaymentCodeScreen
import com.primestrider.kotlinstarter.feature.example.sdk.picker.ImagePickerScreen
import com.primestrider.kotlinstarter.feature.example.sdk.share.ShareScreen
import com.primestrider.kotlinstarter.feature.example.sdk.sharetarget.ShareTargetScreen
import com.primestrider.kotlinstarter.feature.example.sdk.sqlite.TransactionsScreen
import com.primestrider.kotlinstarter.feature.example.sdk.statusbar.StatusBarScreen

/** One Android API demo in the "Device APIs" Home section. [id] is kebab-case and is the whole route. */
data class SdkDemo(
    val id: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val screen: @Composable (onBack: () -> Unit) -> Unit,
)

/**
 * Registry of the demos, in the order they appear on Home. To add one, append an entry here and add its
 * strings; no route, graph or catalog change is needed (see docs/examples.md).
 */
val sdkDemos: List<SdkDemo> =
    listOf(
        SdkDemo("camera", R.string.sdk_camera_title, R.string.sdk_camera_subtitle) { CameraScreen(it) },
        SdkDemo("image-picker", R.string.sdk_picker_title, R.string.sdk_picker_subtitle) { ImagePickerScreen(it) },
        SdkDemo("location", R.string.sdk_location_title, R.string.sdk_location_subtitle) { LocationScreen(it) },
        SdkDemo("biometric", R.string.sdk_biometric_title, R.string.sdk_biometric_subtitle) { BiometricScreen(it) },
        SdkDemo("notifications", R.string.sdk_notifications_title, R.string.sdk_notifications_subtitle) { NotificationsScreen(it) },
        SdkDemo("contacts", R.string.sdk_contacts_title, R.string.sdk_contacts_subtitle) { ContactsScreen(it) },
        SdkDemo("clipboard", R.string.sdk_clipboard_title, R.string.sdk_clipboard_subtitle) { ClipboardScreen(it) },
        SdkDemo("device", R.string.sdk_device_title, R.string.sdk_device_subtitle) { DeviceScreen(it) },
        SdkDemo("share", R.string.sdk_share_title, R.string.sdk_share_subtitle) { ShareScreen(it) },
        SdkDemo("web-browser", R.string.sdk_browser_title, R.string.sdk_browser_subtitle) { WebBrowserScreen(it) },
        SdkDemo("payment-code", R.string.sdk_payment_title, R.string.sdk_payment_subtitle) { PaymentCodeScreen(it) },
        SdkDemo("media", R.string.sdk_media_title, R.string.sdk_media_subtitle) { MediaScreen(it) },
        SdkDemo("sqlite", R.string.sdk_sqlite_title, R.string.sdk_sqlite_subtitle) { TransactionsScreen(it) },
        SdkDemo("files", R.string.sdk_files_title, R.string.sdk_files_subtitle) { FilesScreen(it) },
        SdkDemo("calendar", R.string.sdk_calendar_title, R.string.sdk_calendar_subtitle) { CalendarScreen(it) },
        SdkDemo("app-info", R.string.sdk_app_info_title, R.string.sdk_app_info_subtitle) { AppInfoScreen(it) },
        SdkDemo("background-task", R.string.sdk_background_title, R.string.sdk_background_subtitle) { BackgroundSyncScreen(it) },
        SdkDemo("effects", R.string.sdk_effects_title, R.string.sdk_effects_subtitle) { EffectsScreen(it) },
        SdkDemo("live-photo", R.string.sdk_live_photo_title, R.string.sdk_live_photo_subtitle) { LivePhotoScreen(it) },
        SdkDemo("blob", R.string.sdk_blob_title, R.string.sdk_blob_subtitle) { BlobScreen(it) },
        SdkDemo("status-bar", R.string.sdk_status_bar_title, R.string.sdk_status_bar_subtitle) { StatusBarScreen(it) },
        SdkDemo("music", R.string.sdk_music_title, R.string.sdk_music_subtitle) { MusicScreen(it) },
        SdkDemo("share-target", R.string.sdk_share_target_title, R.string.sdk_share_target_subtitle) { ShareTargetScreen(it) },
        SdkDemo("in-app-update", R.string.sdk_app_update_title, R.string.sdk_app_update_subtitle) { AppUpdateScreen(it) },
        SdkDemo("play-integrity", R.string.sdk_integrity_title, R.string.sdk_integrity_subtitle) { IntegrityScreen(it) },
    )

/** Home entries for the "Device APIs" section, built from [sdkDemos]. */
fun sdkEntries(): List<CatalogEntry> =
    sdkDemos.mapIndexed { index, demo -> CatalogEntry(demo.title, demo.subtitle, SdkDemoRoute(demo.id), CatalogSections.DEVICES, index) }
