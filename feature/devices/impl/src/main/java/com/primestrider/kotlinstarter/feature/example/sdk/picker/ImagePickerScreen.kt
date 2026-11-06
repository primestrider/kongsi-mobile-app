package com.primestrider.kotlinstarter.feature.example.sdk.picker

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.primestrider.kotlinstarter.core.designsystem.component.AppButton
import com.primestrider.kotlinstarter.core.designsystem.component.Banner
import com.primestrider.kotlinstarter.core.designsystem.component.BannerVariant
import com.primestrider.kotlinstarter.core.designsystem.component.ButtonVariant
import com.primestrider.kotlinstarter.core.designsystem.component.InfoCard
import com.primestrider.kotlinstarter.core.designsystem.component.LabeledSlider
import com.primestrider.kotlinstarter.core.designsystem.component.SegmentedChoice
import com.primestrider.kotlinstarter.core.designsystem.component.SwitchRow
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.permission.PermissionStatus
import com.primestrider.kotlinstarter.core.permission.openAppSettings
import com.primestrider.kotlinstarter.core.permission.rememberPermissionRequest
import com.primestrider.kotlinstarter.core.util.currentLocale
import com.primestrider.kotlinstarter.feature.devices.impl.R
import com.primestrider.kotlinstarter.feature.example.ExamplePageScaffold
import com.primestrider.kotlinstarter.feature.example.sdk.SdkSection
import java.io.File
import kotlin.math.roundToInt
import com.primestrider.kotlinstarter.core.ui.R as UiR

/** Photo Picker (images or videos), camera capture and the document picker, with their results. */
@Composable
fun ImagePickerScreen(
    onBack: () -> Unit,
    viewModel: PickerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var source by rememberSaveable { mutableStateOf(MediaSource.IMAGES) }
    var crop by rememberSaveable { mutableStateOf(true) }
    var multiple by rememberSaveable { mutableStateOf(false) }
    var quality by rememberSaveable { mutableFloatStateOf(0.8f) }
    var documentType by rememberSaveable { mutableStateOf(DocumentType.ANY) }
    var cameraTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val options = PickerOptions(source, crop, multiple, quality)

    val single =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onMedia(listOf(it), options) }
        }
    val many =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { viewModel.onMedia(it, options) }
    val documents =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.onDocuments(it) }
    val takePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val target = cameraTarget
            if (saved && target != null) viewModel.onMedia(listOf(Uri.fromFile(File(target))), options.copy(source = MediaSource.IMAGES))
        }

    fun launchCamera() {
        val file = File(context.cacheDir, "camera/${System.currentTimeMillis()}.jpg").also { it.parentFile?.mkdirs() }
        cameraTarget = file.absolutePath
        try {
            takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
        } catch (e: ActivityNotFoundException) {
            viewModel.fail(PickerError.NO_APP)
        }
    }

    // The camera permission is asked for only when the Camera button is pressed; a blocked one offers the settings page.
    val cameraPermission =
        rememberPermissionRequest(Manifest.permission.CAMERA) { granted ->
            if (granted) launchCamera() else viewModel.fail(PickerError.CAMERA_DENIED)
        }

    ExamplePageScaffold(
        title = R.string.sdk_picker_title,
        subtitle = R.string.sdk_picker_subtitle,
        onBack = onBack,
    ) {
        ImagePickerContent(
            state = state,
            options = options,
            documentType = documentType,
            onSource = { source = it },
            onCrop = { crop = it },
            onMultiple = { multiple = it },
            onQuality = { quality = it },
            onDocumentType = { documentType = it },
            onLibrary = {
                val request =
                    PickVisualMediaRequest(
                        if (source == MediaSource.VIDEOS) {
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        } else {
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        },
                    )
                try {
                    if (multiple) many.launch(request) else single.launch(request)
                } catch (e: ActivityNotFoundException) {
                    viewModel.fail(PickerError.NO_APP)
                }
            },
            onCamera = cameraPermission::launch,
            cameraBlocked = cameraPermission.status == PermissionStatus.BLOCKED,
            onOpenSettings = { openAppSettings(context) },
            onChooseFiles = {
                try {
                    documents.launch(arrayOf(documentType.mime))
                } catch (e: ActivityNotFoundException) {
                    viewModel.fail(PickerError.NO_APP)
                }
            },
        )
    }
}

/** The picker's sections for a given [state] and [options]: nothing is launched here, the callbacks do that. */
@Composable
internal fun ImagePickerContent(
    state: PickerState,
    options: PickerOptions,
    documentType: DocumentType,
    onSource: (MediaSource) -> Unit,
    onCrop: (Boolean) -> Unit,
    onMultiple: (Boolean) -> Unit,
    onQuality: (Float) -> Unit,
    onDocumentType: (DocumentType) -> Unit,
    onLibrary: () -> Unit,
    onCamera: () -> Unit,
    onChooseFiles: () -> Unit,
    cameraBlocked: Boolean = false,
    onOpenSettings: () -> Unit = {},
) {
    val locale = currentLocale()
    val sourceLabels = MediaSource.entries.associateWith { stringResource(sourceLabel(it)) }
    val documentLabels = DocumentType.entries.associateWith { stringResource(documentLabel(it)) }

    SdkSection(R.string.sdk_picker_images_title, R.string.sdk_picker_images_description) {
        SegmentedChoice(
            options = MediaSource.entries,
            selected = options.source,
            onSelect = onSource,
            label = { sourceLabels.getValue(it) },
        )
        SwitchRow(
            label = stringResource(R.string.sdk_picker_crop),
            checked = options.crop,
            onCheckedChange = onCrop,
            enabled = !options.multiple,
        )
        SwitchRow(label = stringResource(R.string.sdk_picker_multiple), checked = options.multiple, onCheckedChange = onMultiple)
        LabeledSlider(
            label = stringResource(R.string.sdk_picker_quality),
            value = options.quality,
            onValueChange = { onQuality((it * 10).roundToInt() / 10f) },
            valueRange = 0.1f..1f,
            steps = 8,
            valueText = "%d%%".format(locale, (options.quality * 100).roundToInt()),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppButton(stringResource(R.string.sdk_picker_library), onClick = onLibrary, variant = ButtonVariant.OUTLINED)
            AppButton(stringResource(R.string.sdk_picker_camera), onClick = onCamera, variant = ButtonVariant.OUTLINED)
        }
        state.error?.let { error ->
            val blocked = error == PickerError.CAMERA_DENIED && cameraBlocked
            Banner(
                title = stringResource(errorText(error)),
                message = if (blocked) stringResource(UiR.string.permission_blocked_suffix) else null,
                variant = BannerVariant.ERROR,
            )
            if (blocked) {
                AppButton(
                    stringResource(UiR.string.permission_open_settings),
                    onClick = onOpenSettings,
                    variant = ButtonVariant.OUTLINED,
                )
            }
        }
        if (state.media.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                state.media.forEach { Thumbnail(it) }
            }
            val first = state.media.first()
            InfoCard(
                listOf(
                    stringResource(R.string.sdk_picker_info_type) to
                        stringResource(if (first.isVideo) R.string.sdk_picker_source_videos else R.string.sdk_picker_source_images),
                    stringResource(R.string.sdk_picker_info_size) to first.dimensions,
                    stringResource(R.string.sdk_picker_info_file) to formatKb(first.bytes, locale),
                    stringResource(R.string.sdk_picker_info_mime) to first.mime,
                ),
            )
        }
    }

    SdkSection(R.string.sdk_picker_documents_title, R.string.sdk_picker_documents_description) {
        SegmentedChoice(
            options = DocumentType.entries,
            selected = documentType,
            onSelect = onDocumentType,
            label = { documentLabels.getValue(it) },
        )
        AppButton(stringResource(R.string.sdk_picker_choose), onClick = onChooseFiles)
        if (state.documents.isNotEmpty()) {
            InfoCard(state.documents.map { it.name to formatKb(it.bytes, locale) })
        }
    }
}

@Composable
private fun Thumbnail(media: PickedMedia) {
    val shape = RoundedCornerShape(Radii.md)
    val frame = media.thumbnail
    if (frame != null) {
        Image(
            bitmap = frame.asImageBitmap(),
            contentDescription = stringResource(R.string.sdk_picker_thumbnail),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(104.dp).clip(shape),
        )
    } else if (media.isVideo) {
        // Fallback when no frame could be read.
        Box(
            Modifier.size(104.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.sdk_picker_source_videos), style = MaterialTheme.typography.labelMedium)
        }
    } else {
        AsyncImage(
            model = media.uri,
            contentDescription = stringResource(R.string.sdk_picker_thumbnail),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(104.dp).clip(shape),
        )
    }
}

private fun sourceLabel(source: MediaSource): Int =
    if (source == MediaSource.IMAGES) R.string.sdk_picker_source_images else R.string.sdk_picker_source_videos

private fun documentLabel(type: DocumentType): Int =
    when (type) {
        DocumentType.ANY -> R.string.sdk_picker_doc_any
        DocumentType.PDF -> R.string.sdk_picker_doc_pdf
        DocumentType.IMAGES -> R.string.sdk_picker_doc_images
    }

private fun errorText(error: PickerError): Int =
    when (error) {
        PickerError.CAMERA_DENIED -> R.string.sdk_picker_camera_denied
        PickerError.NO_APP -> R.string.sdk_picker_no_app
        PickerError.READ_FAILED -> R.string.sdk_picker_read_failed
    }

@ThemePreviews
@Composable
private fun ImagePickerScreenPreview() {
    PreviewTheme {
        ExamplePageScaffold(title = R.string.sdk_picker_title, subtitle = R.string.sdk_picker_subtitle, onBack = {}) {
            ImagePickerContent(
                state = PickerState(),
                options = PickerOptions(),
                documentType = DocumentType.ANY,
                onSource = {},
                onCrop = {},
                onMultiple = {},
                onQuality = {},
                onDocumentType = {},
                onLibrary = {},
                onCamera = {},
                onChooseFiles = {},
            )
        }
    }
}
