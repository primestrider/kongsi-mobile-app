package com.primestrider.kongsi.feature.bill.ui.capture

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.primestrider.kongsi.core.designsystem.component.AppButton
import com.primestrider.kongsi.core.designsystem.component.AppScaffold
import com.primestrider.kongsi.core.designsystem.component.Banner
import com.primestrider.kongsi.core.designsystem.component.BannerVariant
import com.primestrider.kongsi.core.designsystem.component.ButtonVariant
import com.primestrider.kongsi.core.designsystem.component.LocalMessenger
import com.primestrider.kongsi.core.designsystem.component.UiText
import com.primestrider.kongsi.core.designsystem.icon.AppIcons
import com.primestrider.kongsi.core.designsystem.theme.PreviewTheme
import com.primestrider.kongsi.core.designsystem.theme.Radii
import com.primestrider.kongsi.core.designsystem.theme.Spacing
import com.primestrider.kongsi.core.designsystem.theme.ThemePreviews
import com.primestrider.kongsi.feature.bill.impl.R

/**
 * How a new bill starts: a photo of the receipt taken with the phone's camera app (no camera permission needed), one
 * from the gallery through the system photo picker (no permission either), or typing it in. [onPhoto] gets the photo
 * to read; while it is read the screen says so, and when nothing came of it, it says why above the choices.
 */
@Composable
fun BillCaptureScreen(
    status: ReadStatus,
    onBack: () -> Unit,
    onPhoto: (Uri) -> Unit,
    onManual: () -> Unit,
    onDismissFailure: () -> Unit,
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    // The file the camera app writes to; kept across process death, since the camera app may run for a while.
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = pending?.toUri() ?: return@rememberLauncherForActivityResult
            pending = null
            if (saved) onPhoto(uri) else ReceiptPhotos.discard(context, uri)
        }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPhoto) }

    CaptureContent(
        status = status,
        onBack = onBack,
        onTakePhoto = {
            onDismissFailure()
            val uri = ReceiptPhotos.newPhotoUri(context)
            pending = uri.toString()
            try {
                camera.launch(uri)
            } catch (e: ActivityNotFoundException) {
                pending = null
                ReceiptPhotos.discard(context, uri)
                messenger.error(UiText.Resource(R.string.bill_capture_no_camera))
            }
        },
        onChoosePhoto = {
            onDismissFailure()
            try {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (e: ActivityNotFoundException) {
                messenger.error(UiText.Resource(R.string.bill_capture_no_gallery))
            }
        },
        onManual = onManual,
    )
}

@Composable
private fun CaptureContent(
    status: ReadStatus,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onManual: () -> Unit,
) {
    AppScaffold(title = stringResource(R.string.bill_capture_title), onBack = onBack) { padding ->
        if (status == ReadStatus.Reading) {
            Reading(Modifier.padding(padding))
            return@AppScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s4, vertical = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            if (status is ReadStatus.Failed) {
                Banner(
                    title =
                        stringResource(
                            when (status.reason) {
                                ReadFailure.UNREADABLE -> R.string.bill_capture_failed_unreadable
                                ReadFailure.NOTHING_FOUND -> R.string.bill_capture_failed_nothing
                            },
                        ),
                    message = stringResource(R.string.bill_capture_failed_action),
                    variant = BannerVariant.WARNING,
                )
            }
            SourceCard(
                icon = AppIcons.Camera,
                title = stringResource(R.string.bill_capture_take_photo),
                body = stringResource(R.string.bill_capture_take_photo_body),
                prominent = true,
                onClick = onTakePhoto,
            )
            SourceCard(
                icon = AppIcons.Photo,
                title = stringResource(R.string.bill_capture_choose_photo),
                body = stringResource(R.string.bill_capture_choose_photo_body),
                prominent = false,
                onClick = onChoosePhoto,
            )
            Text(
                stringResource(R.string.bill_capture_tips),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.s1),
            )
            AppButton(
                text = stringResource(R.string.bill_capture_manual),
                onClick = onManual,
                variant = ButtonVariant.TEXT,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** While the photo is read: a few seconds on the device, so the user knows it is working. */
@Composable
private fun Reading(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(Spacing.s6).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(Spacing.s4, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.bill_capture_reading), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.bill_capture_reading_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** One way to start: a large tappable card, the camera one filled so it reads as the main way. */
@Composable
private fun SourceCard(
    icon: ImageVector,
    title: String,
    body: String,
    prominent: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (prominent) scheme.primaryContainer else scheme.surface
    val content = if (prominent) scheme.onPrimaryContainer else scheme.onSurface
    Surface(
        shape = RoundedCornerShape(Radii.xl),
        color = container,
        contentColor = content,
        border = if (prominent) null else BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 88.dp)
                .padding(Spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            Box(
                Modifier.size(48.dp).background(if (prominent) scheme.primary else scheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (prominent) scheme.onPrimary else scheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = BODY_ALPHA))
            }
        }
    }
}

/** The card's second line, a little quieter than its title. */
private const val BODY_ALPHA = 0.8f

@ThemePreviews
@Composable
private fun BillCaptureScreenPreview() {
    PreviewTheme { CaptureContent(ReadStatus.Idle, onBack = {}, onTakePhoto = {}, onChoosePhoto = {}, onManual = {}) }
}

@ThemePreviews
@Composable
private fun BillCaptureReadingPreview() {
    PreviewTheme { CaptureContent(ReadStatus.Reading, onBack = {}, onTakePhoto = {}, onChoosePhoto = {}, onManual = {}) }
}

@ThemePreviews
@Composable
private fun BillCaptureFailedPreview() {
    PreviewTheme {
        CaptureContent(ReadStatus.Failed(ReadFailure.NOTHING_FOUND), onBack = {}, onTakePhoto = {}, onChoosePhoto = {}, onManual = {})
    }
}
